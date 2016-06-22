package org.globalnames
package resolver

import java.util.UUID

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.globalnames.resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._
import org.apache.commons.lang3.StringUtils.capitalize

class Resolver(db: Database, matcher: Matcher) {
  import Resolver.Kind._
  import Resolver.{Matches, Match}

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

  def resolve(name: String,
              take: Int = 1000, drop: Int = 0): Future[Matches] = {
    val nameUuid = gen.generate(capitalize(name))
    val exactMatches = nameStrings.filter { ns =>
      ns.id === nameUuid || ns.canonicalUuid === nameUuid
    }

    def handle(canonicalNameParts: Seq[String]): Future[Matches] = {
      if (canonicalNameParts.isEmpty) {
        Future.successful(Matches(0, Seq()))
      } else {
        val canonicalName = canonicalNameParts.mkString(" ")
        val canonicalNameUuid = gen.generate(canonicalName)
        val exactNameStringsByCanonical = nameStrings.filter { ns =>
          ns.id === canonicalNameUuid || ns.canonicalUuid === canonicalNameUuid
        }
        db.run(exactNameStringsByCanonical.result).flatMap { exactNameStrings =>
          if (exactNameStrings.isEmpty) {
            val fuzzyMatchMap =
              matcher.transduce(canonicalName)
                     .map { candidate => gen.generate(candidate.term) -> candidate }
                     .toMap
            if (fuzzyMatchMap.isEmpty) {
              handle(canonicalNameParts.dropRight(1))
            } else {
              val fuzzyNameStringsQuery =
                nameStrings.filter { ns => ns.canonicalUuid.inSetBind(fuzzyMatchMap.keys) }
              val fuzzyNameStrings =
                db.run(fuzzyNameStringsQuery.drop(drop).take(take).result)
                  .map { ns => ns.map { n =>
                    Match(n, Fuzzy(fuzzyMatchMap(n.canonicalName.get.id).distance))
                  }}
              for {
                count <- db.run(fuzzyNameStringsQuery.size.result)
                fns <- fuzzyNameStrings
              } yield Matches(count, fns)
            }
          } else {
            db.run(exactNameStringsByCanonical.size.result).map { canonicalsCount =>
              Matches(canonicalsCount, exactNameStrings.map { n => Match(n) })
            }
          }
        }
      }
    }

    db.run(exactMatches.size.result).flatMap { exactMatchesCount =>
      if (exactMatchesCount == 0) {
        val canonicalName = snp.fromString(name).canonized().orZero
        handle(canonicalName.split(' '))
      } else {
        db.run(exactMatches.drop(drop).take(take).result).map { ns =>
          Matches(exactMatchesCount, ns.map { n => Match(n) })
        }
      }
    }
  }

  def resolveCanonical(canonicalName: String,
                       take: Int, drop: Int): Future[Matches] = {
    val canonicalNameUuid = gen.generate(canonicalName)
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonicalUuid === canonicalNameUuid
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveCanonicalLike(canonicalName: String,
                           take: Int, drop: Int): Future[Matches] = {
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonical.like(canonicalName)
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveAuthor(authorName: String,
                    take: Int, drop: Int): Future[Matches] = {
    val query = authorWords.filter { x => x.authorWord === authorName }
                           .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveYear(year: String,
                  take: Int, drop: Int): Future[Matches] = {
    val query = yearWords.filter { x => x.yearWord === year }
                         .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveUninomial(uninomial: String,
                       take: Int, drop: Int): Future[Matches] = {
    val query = uninomialWords.filter { x => x.uninomialWord === uninomial }
                              .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveGenus(genus: String,
                   take: Int, drop: Int): Future[Matches] = {
    val query = genusWords.filter { x => x.genusWord === genus }
                          .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveSpecies(species: String,
                     take: Int, drop: Int): Future[Matches] = {
    val query = speciesWords.filter { x => x.speciesWord === species }
                            .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveSubspecies(subspecies: String,
                        take: Int, drop: Int): Future[Matches] = {
    val query = subspeciesWords.filter { x => x.subspeciesWord === subspecies }
                               .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) })
  }

  def resolveDataSources(uuid: UUID): Future[Seq[(NameStringIndex, DataSource)]] = {
    val query = for {
      nsi <- nameStringIndicies
      ds <- dataSources
      if nsi.nameStringId === uuid && nsi.dataSourceId === ds.id
    } yield (nsi, ds)

    db.run(query.result)
  }
}

object Resolver {
  sealed trait Kind
  object Kind {
    case object None extends Kind

    case class Fuzzy(score: Int) extends Kind
  }

  case class Match(nameString: NameString, kind: Kind = Kind.None)
  case class Matches(total: Long, matches: Seq[Match])
}
