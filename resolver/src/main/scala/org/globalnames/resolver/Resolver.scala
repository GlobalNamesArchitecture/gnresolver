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
    nameStrings.filter { ns =>
      ns.id === nameUuid ||
        (ns.canonicalUuid =!= NameStrings.emptyCanonicalUuid &&
          ns.canonicalUuid === canonicalNameUuid)
    }
  }

  private def gnmatchCanonicals(
      canonicalNamePartsNonEmpty: Seq[(String, Array[String], Option[SuppliedId])]) = {
    val canonicalNamesFuzzy = canonicalNamePartsNonEmpty.map { case (name, parts, suppliedId) =>
      val candsUuids: Seq[UUID] = if (parts.length > 1) {
        val can = parts.mkString(" ")
        matcher.transduce(can).map { c => gen.generate(c.term) }
      } else {
        parts.map { p => gen.generate(p) }
      }
      (name, parts, candsUuids, suppliedId)
    }
    val fuzzyMatchLimit = 5
    val (foundFuzzyMatches, unfoundFuzzyMatches) =
      canonicalNamesFuzzy.partition { case (_, _, cands, _) =>
        cands.nonEmpty && cands.size <= fuzzyMatchLimit
      }
    (foundFuzzyMatches, unfoundFuzzyMatches)
  }

  private def fuzzyMatch(canonicalNameParts: Seq[(String, Array[String], Option[SuppliedId])],
                         dataSourceIds: Seq[Int], parameters: Parameters): Future[Seq[Matches]] = {
    val canonicalNamePartsNonEmpty = canonicalNameParts.filter {
      case (_, parts, _) => parts.nonEmpty
    }
    if (canonicalNamePartsNonEmpty.isEmpty) {
      Future.successful(Seq())
    } else {
      val (foundFuzzyMatches, unfoundFuzzyMatches) = gnmatchCanonicals(canonicalNamePartsNonEmpty)
      val unfoundFuzzyResult =
        fuzzyMatch(unfoundFuzzyMatches.map { case (name, parts, _, suppliedId) =>
          (name, parts.dropRight(1), suppliedId)
        }, dataSourceIds, parameters)

      val fuzzyNameStringsMaxCount = 50
      val queryFoundFuzzy = nameStringsSequenceMatches(
        foundFuzzyMatches.map { case (verbatim, _, candUuids, _) =>
          val ns = nameStrings.filter { ns =>
            ns.canonicalUuid =!= NameStrings.emptyCanonicalUuid &&
              ns.canonicalUuid.inSetBind(candUuids)
          }
          val params = parameters.copy(query = verbatim.some, perPage = fuzzyNameStringsMaxCount,
                                       matchType = MatchType.Fuzzy)
          (ns, params)
        })

      val foundFuzzyResult = queryFoundFuzzy.flatMap { fuzzyMatches =>
        val (matched, unmatched) =
          foundFuzzyMatches.zip(fuzzyMatches).partition { case (_, m) => m.matches.nonEmpty }
        val matchedResult = matched.map { case ((name, _, _, suppliedId), m) =>
          val matchesInDataSources = m.matches
            .filter { m =>
              dataSourceIds.isEmpty || dataSourceIds.contains(m.nameStringIndex.dataSourceId)
            }
          m.copy(matches = matchesInDataSources, suppliedId = suppliedId)
        }
        val matchedFuzzyResult = {
          val unmatchedParts = unmatched.map { case ((name, parts, _, suppliedId), _) =>
            (name, parts.dropRight(1), suppliedId)
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
    val namesCapital = names.map { n => NameRequest(capitalize(n.value), n.suppliedId) }

    val nameStringsPerFuture = 200
    val scientificNamesFuture = Future.sequence(
      namesCapital.grouped(nameStringsPerFuture).map { namesGp =>
        Future { namesGp.map { name => (snp.fromString(name.value), name.suppliedId) } }
      }).map { x => x.flatten.toList }

    scientificNamesFuture.flatMap { scientificNamesIds =>
      val exactMatches = scientificNamesIds.grouped(scientificNamesIds.size / 50 + 1).map { snIds =>
        val qry = snIds.map { case (sn, _) =>
          val canUuid: UUID = sn.canonizedUuid().map { _.id }.getOrElse(gen.generate(""))
          val ns = exactNamesQuery(sn.input.id, canUuid)
          val pms = parameters.copy(query = sn.input.verbatim.some)
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

        val matchesResult = matched.map { case (mtch, (sn, suppliedId)) =>
          val ms = mtch.matches
              .filter { m =>
                dataSourceIds.isEmpty || dataSourceIds.contains(m.nameStringIndex.dataSourceId)
              }.map { m =>
                val mt: MatchType =
                  if (m.nameString.name.id == sn.input.id) MatchType.ExactNameMatchByUUID
                  else MatchType.ExactCanonicalNameMatchByUUID
                m.copy(matchType = mt)
              }
          mtch.copy(matches = ms, suppliedId = suppliedId)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }
}

object Resolver {
  case class NameRequest(value: String, suppliedId: Option[SuppliedId])
}
