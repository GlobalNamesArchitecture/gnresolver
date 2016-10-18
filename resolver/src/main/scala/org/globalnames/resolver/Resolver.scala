package org.globalnames
package resolver

import java.util.UUID

import org.apache.commons.lang3.StringUtils.capitalize
import parser.ScientificNameParser.{instance => snp}
import resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

class Resolver(db: Database, matcher: Matcher) {
  import Resolver.NameRequest

  private val gen = UuidGenerator()
  private val nameStrings = TableQuery[NameStrings]
  private val nameStringIndicies = TableQuery[NameStringIndices]

  private def exactNamesQuery(nameUuid: Rep[UUID], canonicalNameUuid: Rep[UUID]) = {
    val exactNamesMaxCount = 50
    nameStrings.filter { nameString =>
      nameString.id === nameUuid || nameString.canonicalUuid === canonicalNameUuid
    }.join(nameStringIndicies).on { _.id === _.nameStringId }
     .take(exactNamesMaxCount)
  }

  private val exactNamesQueryCompiled = Compiled(exactNamesQuery _)

  private def gnmatchCanonicals(
      canonicalNamePartsNonEmpty: Seq[(String, Array[String], Option[LocalId])]) = {
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

  private def fuzzyMatch(canonicalNameParts: Seq[(String, Array[String], Option[LocalId])],
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
            }.map { case (ns, nsi) => Match(ns, nsi.dataSourceId, Kind.Fuzzy) }
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

  def resolveString(name: String, take: Int, drop: Int): Future[Matches] = {
    resolveStrings(Seq(name)).map { _.head }
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
              }.map { case (ns, nsi) =>
                val kind: Kind = if (ns.name.id == sn.input.id) Kind.ExactNameMatchByUUID
                                 else Kind.ExactCanonicalNameMatchByUUID
                Match(ns, nsi.dataSourceId, kind)
              }
          Matches(matches.size, matches, sn.input.verbatim, localId)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }
}

object Resolver {
  case class NameRequest(value: String, localId: Option[LocalId])
}
