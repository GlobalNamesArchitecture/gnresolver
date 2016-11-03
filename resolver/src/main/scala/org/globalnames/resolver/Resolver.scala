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

class Resolver(val db: Database, matcher: Matcher) extends Materializer {
  import Resolver.NameRequest
  import Materializer.Parameters

  private val gen = UuidGenerator()

  private def exactNamesQuery(nameUuid: Rep[UUID], canonicalNameUuid: Rep[UUID]) = {
    nameStrings.filter { nameString =>
      nameString.id === nameUuid || nameString.canonicalUuid === canonicalNameUuid
    }
  }

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
                         dataSourceIds: Seq[Int], parameters: Parameters): Future[Seq[Matches]] = {
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
        }, dataSourceIds, parameters)

      val fuzzyNameStringsMaxCount = 50
      val queryFoundFuzzy = nameStringsSequenceMatches(
        foundFuzzyMatches.map { case (verbatim, _, candUuids, _) =>
          val ns = nameStrings.filter { ns => ns.canonicalUuid.inSetBind(candUuids) }
          val params = parameters.copy(query = verbatim, perPage = fuzzyNameStringsMaxCount,
                                       kind = Kind.Fuzzy)
          (ns, params)
        })

      val foundFuzzyResult = queryFoundFuzzy.flatMap { fuzzyMatches =>
        val (matched, unmatched) =
          foundFuzzyMatches.zip(fuzzyMatches).partition { case (_, m) => m.matches.nonEmpty }
        val matchedResult = matched.map { case ((name, _, _, localId), m) =>
          val matchesInDataSources = m.matches
            .filter { m =>
              dataSourceIds.isEmpty || dataSourceIds.contains(m.nameStringIndex.dataSourceId)
            }
          m.copy(matches = matchesInDataSources, localId = localId)
        }
        val matchedFuzzyResult = {
          val unmatchedParts = unmatched.map { case ((name, parts, _, localId), _) =>
            (name, parts.dropRight(1), localId)
          }
          fuzzyMatch(unmatchedParts, dataSourceIds, parameters)
        }
        matchedFuzzyResult.map { mfr => matchedResult ++ mfr }
      }

      for (ufr <- unfoundFuzzyResult; ffr <- foundFuzzyResult) yield ufr ++ ffr
    }
  }

  def resolveString(name: String, parameters: Parameters): Future[Matches] = {
    resolveStrings(Seq(name), parameters).map { x =>
      x.headOption.getOrElse(Matches.empty(name))
    }
  }

  def resolveStrings(names: Seq[String], parameters: Parameters): Future[Seq[Matches]] = {
    val nrs = names.map { n => NameRequest(n, None) }
    resolve(nrs, Vector(), parameters)
  }

  def resolve(names: Seq[NameRequest], dataSourceIds: Vector[Int],
              parameters: Parameters): Future[Seq[Matches]] = {
    val namesCapital = names.map { n => NameRequest(capitalize(n.value), n.localId) }

    val nameStringsPerFuture = 200
    val scientificNamesFuture = Future.sequence(
      namesCapital.grouped(nameStringsPerFuture).map { namesGp =>
        Future { namesGp.map { name => (snp.fromString(name.value), name.localId) } }
      }).map { x => x.flatten.toList }

    scientificNamesFuture.flatMap { scientificNamesIds =>
      val exactMatches = scientificNamesIds.grouped(scientificNamesIds.size / 50 + 1).map { snIds =>
        val qry = snIds.map { case (sn, _) =>
          val canUuid: UUID = sn.canonizedUuid().map { _.id }.getOrElse(gen.generate(""))
          val ns = exactNamesQuery(sn.input.id, canUuid)
          val pms = parameters.copy(query = sn.input.verbatim)
          (ns, pms)
        }
        nameStringsSequenceMatches(qry)
      }

      Future.sequence(exactMatches).flatMap { exactMatchesChunks =>
        val exactMatches = exactMatchesChunks.flatten.toList

        val (matched, unmatched) = exactMatches.zip(scientificNamesIds).partition {
          case (m, _) => m.matches.nonEmpty
        }

        val parts = unmatched.map { case (_, (sn, localId)) =>
          (sn.input.verbatim, sn.canonized().orZero.split(' '), localId)
        }
        val fuzzyMatches = fuzzyMatch(parts, dataSourceIds, parameters)

        val matchesResult = matched.map { case (mtch, (sn, localId)) =>
          val ms = mtch.matches
              .filter { m =>
                dataSourceIds.isEmpty || dataSourceIds.contains(m.nameStringIndex.dataSourceId)
              }.map { m =>
                val k: Kind = if (m.nameString.name.id == sn.input.id) Kind.ExactNameMatchByUUID
                              else Kind.ExactCanonicalNameMatchByUUID
                m.copy(kind = k)
              }
          mtch.copy(matches = ms, localId = localId)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }
}

object Resolver {
  case class NameRequest(value: String, localId: Option[LocalId])
}
