package org.globalnames
package resolver

import java.util.UUID

import org.apache.commons.lang3.StringUtils.capitalize
import parser.ScientificNameParser.{Result => SNResult, instance => snp}
import resolver.model._
import org.slf4j.LoggerFactory
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

class Resolver(val db: Database, matcher: Matcher) extends Materializer {
  import Materializer.Parameters
  import Resolver.NameRequest

  private val gen = UuidGenerator()
  private val emptyUuid = gen.generate("")
  private val logger = LoggerFactory.getLogger(getClass)

  private def exactNamesQuery(nameUuid: Rep[UUID], canonicalNameUuid: Rep[UUID]) = {
    nameStrings.filter { ns =>
      ns.id === nameUuid || ns.canonicalUuid === canonicalNameUuid
    }
  }

  private def exactNamesQueryWildcard(query: Rep[String]) = {
    nameStrings.filter { ns => ns.name.like(query) || ns.canonical.like(query) }
  }

  private def gnmatchCanonicals(
       canonicalNamePartsNonEmpty: Seq[(SNResult, Array[String], Option[SuppliedId])]) = {
    val canonicalNamesFuzzy = canonicalNamePartsNonEmpty.map { case (name, parts, suppliedId) =>
      val candsUuids: Seq[(UUID, MatchType)] = if (parts.length > 1) {
        val can = parts.mkString(" ")
        matcher.transduce(can).map { c => (gen.generate(c.term), MatchType.Fuzzy) }
      } else {
        parts.map { p => (gen.generate(p), MatchType.ExactMatchPartialByGenus) }
      }
      (name, parts, candsUuids, suppliedId)
    }
    val fuzzyMatchLimit = 5
    canonicalNamesFuzzy.partition { case (_, _, cands, _) =>
      cands.nonEmpty && cands.size <= fuzzyMatchLimit
    }
  }

  private def fuzzyMatch(canonicalNameParts: Seq[(SNResult, Array[String], Option[SuppliedId])],
                         dataSourceIds: Seq[Int], parameters: Parameters): Future[Seq[Matches]] = {
    val canonicalNamePartsNonEmpty = canonicalNameParts.filter {
      case (_, parts, _) => parts.nonEmpty
    }
    if (canonicalNamePartsNonEmpty.isEmpty) {
      Future.successful(Seq())
    } else {
      val (foundFuzzyMatches, unfoundFuzzyMatches) = gnmatchCanonicals(canonicalNamePartsNonEmpty)
      val unfoundFuzzyResult = {
        val unfoundFuzzyCanonicalNameParts = unfoundFuzzyMatches.map {
          case (name, parts, _, suppliedId) => (name, parts.dropRight(1), suppliedId)
        }
        fuzzyMatch(unfoundFuzzyCanonicalNameParts, dataSourceIds, parameters)
      }

      val fuzzyNameStringsMaxCount = 5
      val queryFoundFuzzy: Future[Seq[Matches]] = {
        val nameStringsQueries = foundFuzzyMatches.flatMap { case (sn, _, candUuids, _) =>
          candUuids.filter { case (u, _) => u != NameStrings.emptyCanonicalUuid }
                   .map { case (uuid, matchType) =>
            val ns = nameStrings.filter { ns =>
              ns.canonicalUuid === uuid
            }
            val params = parameters.copy(query = sn.input.verbatim.some,
                                         perPage = fuzzyNameStringsMaxCount,
                                         matchType = matchType)
            (ns, params)
          }
        }
        nameStringsSequenceMatches(nameStringsQueries)
      }

      val foundFuzzyResult = queryFoundFuzzy.flatMap { fuzzyMatches =>
        val (matched, unmatched) =
          foundFuzzyMatches.zip(fuzzyMatches).partition { case (_, m) => m.matches.nonEmpty }
        val matchedResult = matched.map { case ((name, _, _, suppliedId), mtchs) =>
          val matchesInDataSources = mtchs.matches
            .filter { m =>
              dataSourceIds.isEmpty || dataSourceIds.contains(m.nameStringIndex.dataSourceId)
            }
          mtchs.copy(matches = matchesInDataSources, suppliedId = suppliedId,
                     scientificName = name.some)
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

  def resolveString(name: String, parameters: Parameters, wildcard: Boolean): Future[Matches] = {
    resolveHelper(Seq((NameRequest(name, None), wildcard)), Vector(), parameters).map { x =>
      x.headOption.getOrElse(Matches.empty(name))
    }
  }

  private def resolveHelper(names: Seq[(NameRequest, Boolean)], dataSourceIds: Vector[Int],
                            parameters: Parameters): Future[Seq[Matches]] = {
    val (namesWc, namesExact) = names.partition { case (_, wildcard) => wildcard }
    for {
      nwc <- resolveWildcard(namesWc.map { case (ns, _) => ns }, dataSourceIds, parameters)
      nex <- resolveExact(namesExact.map { case (ns, _) => ns }, dataSourceIds, parameters)
    } yield nwc ++ nex
  }

  private def resolveWildcard(names: Seq[NameRequest], dataSourceIds: Vector[Int],
                              parameters: Parameters): Future[Seq[Matches]] = {
    val qrys = names.map { name =>
      val capName = capitalize(name.value) + "%"
      (exactNamesQueryWildcard(capName), parameters)
    }
    nameStringsSequenceMatches(qrys)
  }

  /**
    * Algorithm:
    * parsedNames = parse in parallel with snp.fromString
    * dbResult = query DB by parsedNames
    *   if canonical is empty then by name only
    *   else by name and canonical
    * (matched, unmatched) = split dbResult: returned values are non-empty or empty
    * fuzzyResult = make fuzzy match on unmatched
    * return: matched ++ fuzzyResult
    *
    * TODO:
    *   5, 4, 3, 2 words -> ExactMatchPartial (in case of subspecies - drop middle words)
    *   If single word is a Genus then match it -> ExactMatchPartialByGenus
    */
  def resolveExact(names: Seq[NameRequest], dataSourceIds: Vector[Int],
                   parameters: Parameters): Future[Seq[Matches]] = {
    val namesCapital = names.map { n => n.copy(value = capitalize(n.value)) }

    val nameStringsPerFuture = 200
    val scientificNamesFuture = Future.sequence(
      namesCapital.grouped(nameStringsPerFuture).map { namesGp =>
        Future { namesGp.map { name => (snp.fromString(name.value), name.suppliedId) } }
      }).map { x => x.flatten.toList }

    scientificNamesFuture.flatMap { scientificNamesIds =>
      val exactMatches = scientificNamesIds.grouped(nameStringsPerFuture).map { snIds =>
        val qry = snIds.map { case (sn, _) =>
          val canUuid: UUID = sn.canonizedUuid().map { _.id }.getOrElse(emptyUuid)
          val ns = if (canUuid == NameStrings.emptyCanonicalUuid) {
            nameStrings.filter { ns => ns.id === sn.input.id }
          } else {
            exactNamesQuery(sn.input.id, canUuid)
          }
          val pms = parameters.copy(query = sn.input.verbatim.some, perPage = 5)
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
          (sn, sn.canonized().orZero.split(' '), localId)
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
          mtch.copy(matches = ms, suppliedId = suppliedId, scientificName = sn.some)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }
}

object Resolver {
  case class NameRequest(value: String, suppliedId: Option[SuppliedId])
}
