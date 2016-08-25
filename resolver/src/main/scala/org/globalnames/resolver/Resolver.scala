package org.globalnames
package resolver

import java.util.UUID

import org.apache.commons.lang3.StringUtils.capitalize
import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.globalnames.resolver.Resolver.NameRequest
import org.globalnames.resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

class Resolver(db: Database, matcher: Matcher) {
  import Resolver.{Match, Matches, LocalId, Kind}

  val gen = UuidGenerator()
  val nameStrings = TableQuery[NameStrings]
  val authorWords = TableQuery[AuthorWords]
  val uninomialWords = TableQuery[UninomialWords]
  val genusWords = TableQuery[GenusWords]
  val speciesWords = TableQuery[SpeciesWords]
  val subspeciesWords = TableQuery[SubspeciesWords]
  val yearWords = TableQuery[YearWords]
  val dataSources = TableQuery[DataSources]
  val nameStringIndicies = TableQuery[NameStringIndices]
  val crossMaps = TableQuery[CrossMaps]

  private def exactNamesQuery(nameUuid: Rep[UUID], canonicalNameUuid: Rep[UUID]) = {
    val exactNamesMaxCount = 50
    nameStrings.filter { nameString =>
      nameString.id === nameUuid || nameString.canonicalUuid === canonicalNameUuid
    }.join(nameStringIndicies).on { _.id === _.nameStringId }
     .take(exactNamesMaxCount)
  }

  private val exactNamesQueryCompiled = Compiled(exactNamesQuery _)

  private def gnmatchCanonicals(
      canonicalNamePartsNonEmpty: Seq[(String, Array[String], LocalId)]) = {
    val canonicalNamesFuzzy = canonicalNamePartsNonEmpty.map { case (name, parts, localId) =>
      val candsUuids: Seq[UUID] = if (parts.length > 1) {
        val can = parts.mkString(" ")
        matcher.transduce(can).map { c => gen.generate(c.term) }
      } else {
        parts.map { p => gen.generate(p) }
      }
      (name, parts, candsUuids, localId)
    }
    val fuzzyMatchLimit = 5
    val (foundFuzzyMatches, unfoundFuzzyMatches) =
      canonicalNamesFuzzy.partition { case (_, _, cands, _) =>
        cands.nonEmpty && cands.size <= fuzzyMatchLimit
      }
    (foundFuzzyMatches, unfoundFuzzyMatches)
  }

  private def fuzzyMatch(canonicalNameParts: Seq[(String, Array[String], LocalId)],
                         dataSourceIds: Seq[Int]): Future[Seq[Matches]] = {
    val canonicalNamePartsNonEmpty = canonicalNameParts.filter {
      case (_, parts, _) => parts.nonEmpty
    }
    if (canonicalNamePartsNonEmpty.isEmpty) {
      Future.successful(Seq())
    } else {
      val (foundFuzzyMatches, unfoundFuzzyMatches) = gnmatchCanonicals(canonicalNamePartsNonEmpty)
      val unfoundFuzzyResult =
        fuzzyMatch(unfoundFuzzyMatches.map { case (name, parts, _, localId) =>
          (name, parts.dropRight(1), localId)
        }, dataSourceIds)

      val fuzzyNameStringsMaxCount = 50
      val queryFoundFuzzy = DBIO.sequence(foundFuzzyMatches.map { case (_, _, candUuids, _) =>
        nameStrings.filter { ns => ns.canonicalUuid.inSetBind(candUuids) }
                   .join(nameStringIndicies).on { _.id === _.nameStringId }
                   .take(fuzzyNameStringsMaxCount)
                   .result
      })

      val foundFuzzyResult = db.run(queryFoundFuzzy).flatMap { fuzzyMatches =>
        val (matched, unmatched) =
          foundFuzzyMatches.zip(fuzzyMatches).partition { case (_, matches) => matches.nonEmpty }
        val matchedResult = matched.map { case ((name, _, _, localId), matches) =>
          val matchesInDataSources = matches
            .filter { case (ns, nsi) =>
              dataSourceIds.isEmpty || dataSourceIds.contains(nsi.dataSourceId)
            }.map { case (ns, nsi) => Match(ns, nsi.dataSourceId, Kind.Fuzzy(0)) }
          Matches(matchesInDataSources.size, matchesInDataSources, name, localId)
        }
        val matchedFuzzyResult = {
          val unmatchedParts = unmatched.map { case ((name, parts, _, localId), _) =>
            (name, parts.dropRight(1), localId)
          }
          fuzzyMatch(unmatchedParts, dataSourceIds)
        }
        matchedFuzzyResult.map { mfr => matchedResult ++ mfr }
      }

      for (ufr <- unfoundFuzzyResult; ffr <- foundFuzzyResult) yield ufr ++ ffr
    }
  }

  def resolveStrings(names: Seq[String]): Future[Seq[Matches]] = {
    resolve(names.map { n => NameRequest(n, None) }, Vector())
  }

  def resolve(names: Seq[NameRequest], dataSourceIds: Vector[Int]): Future[Seq[Matches]] = {
    val namesCapital = names.map { n => NameRequest(capitalize(n.value), n.localId) }

    val nameStringsPerFuture = 200
    val scientificNamesFuture = Future.sequence(
      namesCapital.grouped(nameStringsPerFuture).map { namesGp =>
        Future { namesGp.map { name => (snp.fromString(name.value), name.localId) } }
      }).map { x => x.flatten.toList }

    scientificNamesFuture.flatMap { scientificNamesIds =>
      val exactMatches = scientificNamesIds.grouped(scientificNamesIds.size / 50 + 1).map { snIds =>
        val qry =
          DBIO.sequence(snIds.map { case (sn, _) =>
            val canUuid: UUID = sn.canonizedUuid().map { _.id }.getOrElse(gen.generate(""))
            exactNamesQueryCompiled((sn.input.id, canUuid)).result
          })
        db.run(qry)
      }

      Future.sequence(exactMatches).flatMap { exactMatchesChunks =>
        val exactMatches = exactMatchesChunks.flatten.toList

        val (matched, unmatched) = exactMatches.zip(scientificNamesIds).partition {
          case (matchedNameStrings, _) => matchedNameStrings.nonEmpty
        }

        val parts = unmatched.map { case (_, (sn, localId)) =>
          (sn.input.verbatim, sn.canonized().orZero.split(' '), localId)
        }
        val fuzzyMatches = fuzzyMatch(parts, dataSourceIds)

        val matchesResult = matched.map { case (matchedNameStrings, (sn, localId)) =>
          val matches = matchedNameStrings
              .filter { case (ns, nsi) =>
                dataSourceIds.isEmpty || dataSourceIds.contains(nsi.dataSourceId)
              }.map { case (ns, nsi) => Match(ns, nsi.dataSourceId, Kind.Fuzzy(0)) }
          Matches(matches.size, matches, sn.input.verbatim, localId)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }

  def resolveCanonical(canonicalName: String, take: Int, drop: Int): Future[Matches] = {
    val canonicalNameUuid = gen.generate(canonicalName)
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonicalUuid === canonicalNameUuid
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, canonicalName)
  }

  def resolveCanonicalLike(canonicalName: String, take: Int, drop: Int): Future[Matches] = {
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonical.like(canonicalName)
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, canonicalName)
  }

  def resolveAuthor(authorName: String, take: Int, drop: Int): Future[Matches] = {
    val query = authorWords.filter { x => x.authorWord === authorName }
                           .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, authorName)
  }

  def resolveYear(year: String, take: Int, drop: Int): Future[Matches] = {
    val query = yearWords.filter { x => x.yearWord === year }
                         .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, year)
  }

  def resolveUninomial(uninomial: String, take: Int, drop: Int): Future[Matches] = {
    val query = uninomialWords.filter { x => x.uninomialWord === uninomial }
                              .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, uninomial)
  }

  def resolveGenus(genus: String, take: Int, drop: Int): Future[Matches] = {
    val query = genusWords.filter { x => x.genusWord === genus }
                          .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, genus)
  }

  def resolveSpecies(species: String, take: Int, drop: Int): Future[Matches] = {
    val query = speciesWords.filter { x => x.speciesWord === species }
                            .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, species)
  }

  def resolveSubspecies(subspecies: String, take: Int, drop: Int): Future[Matches] = {
    val query = subspeciesWords.filter { x => x.subspeciesWord === subspecies }
                               .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, subspecies)
  }

  def resolveDataSources(uuid: UUID): Future[Seq[(NameStringIndex, DataSource)]] = {
    val query = for {
      nsi <- nameStringIndicies
      ds <- dataSources
      if nsi.nameStringId === uuid && nsi.dataSourceId === ds.id
    } yield (nsi, ds)

    db.run(query.result)
  }

  def crossMap(databaseSourceId: Int, databaseTargetId: Int,
               localIds: Seq[String]): Future[Seq[(String, String)]] = {
    // Slick doesn't give facilities to create temporary table from `localIds` and
    // make join against it. Until https://github.com/slick/slick/issues/799 is solved
    val existingCrossMapsQuery = crossMaps.filter { cm =>
      cm.dataSourceId === databaseSourceId && cm.localId.inSetBind(localIds)
    }
    val existingLocalIds = db.run(existingCrossMapsQuery.map { _.localId }.result)

    val query = for {
      sourceCrossMap <- existingCrossMapsQuery
      mapping <- nameStringIndicies.filter { nsi =>
        nsi.nameStringId === sourceCrossMap.nameStringId
      }
      targetCrossMap <- crossMaps.filter { cm =>
        cm.dataSourceId === databaseTargetId && cm.nameStringId === mapping.nameStringId
      }
    } yield (sourceCrossMap.localId, targetCrossMap.localId)

    for {
      mapped <- db.run(query.result).map { _.distinct }
      existing <- existingLocalIds
    } yield mapped ++ (localIds diff existing).map { (_, "") }
  }

  def findNameStringByUuid(uuid: UUID): Future[Matches] = {
    val query = nameStrings.filter { ns => ns.id === uuid }.take(1)
    db.run(query.result).map { xs => Matches(xs.size, xs.map { x => Match(x) }, uuid.toString) }
  }
}

object Resolver {
  type LocalId = Option[Int]

  case class NameRequest(value: String, localId: LocalId)

  sealed trait Kind
  object Kind {
    case object None extends Kind

    case class Fuzzy(score: Int) extends Kind
  }

  case class Match(nameString: NameString, dataSourceId: Int = 0, kind: Kind = Kind.None)
  case class Matches(total: Long, matches: Seq[Match], query: String, localId: LocalId = None)

  object Matches {
    val empty = Matches(0, Seq(), "")
  }
}