package org.globalnames
package resolver

import java.util.UUID

import org.apache.commons.lang3.StringUtils.capitalize
import model.db.NameStrings
import parser.ScientificNameParser.{Result => SNResult, instance => snp}
import resolver.model._
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

class Resolver(val database: Database, matcher: Matcher) extends Materializer {
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

  private case class CanonicalNameSplit(result: SNResult,
                                        parts: List[String],
                                        suppliedId: Option[SuppliedId])

  private def gnmatchCanonicals(canonicalNamePartsNonEmpty: Seq[CanonicalNameSplit]) = {
    val canonicalNamesFuzzy = canonicalNamePartsNonEmpty.map { cnp =>
      val candsUuids: Seq[(UUID, MatchType)] =
        if (cnp.parts.length > 1) {
          val can = cnp.parts.mkString(" ")
          matcher.transduce(can).map { c =>
            val matchType: MatchType =
              if (c.distance == 0) MatchType.ExactPartialMatch
              else MatchType.Fuzzy
            (gen.generate(c.term), matchType)
          }
        } else {
          cnp.parts.map { p => (gen.generate(p), MatchType.ExactMatchPartialByGenus) }
        }
      (cnp, candsUuids)
    }
    val fuzzyMatchLimit = 5
    canonicalNamesFuzzy.partition { case (cnp, cands) =>
      cands.nonEmpty && cands.size <= fuzzyMatchLimit
    }
  }

  private def fuzzyMatch(canonicalNameParts: Seq[CanonicalNameSplit],
                         parameters: Parameters): Future[Seq[Matches]] = {
    val (canonicalNamePartsNonEmpty, canonicalNamePartsEmpty) = canonicalNameParts.partition {
      cnp => cnp.parts.nonEmpty
    }
    val emptyMatches = canonicalNamePartsEmpty.map { cnp =>
      Matches.empty(cnp.result.input.verbatim, cnp.suppliedId)
    }
    if (canonicalNamePartsNonEmpty.isEmpty) {
      Future.successful(emptyMatches)
    } else {
      val (foundFuzzyMatches, unfoundFuzzyMatches) = gnmatchCanonicals(canonicalNamePartsNonEmpty)
      val unfoundFuzzyResult = {
        val unfoundFuzzyCanonicalNameParts = unfoundFuzzyMatches.map {
          case (cnp, _) => cnp.copy(parts = cnp.parts.dropRight(1))
        }
        fuzzyMatch(unfoundFuzzyCanonicalNameParts, parameters)
      }

      val fuzzyNameStringsMaxCount = 5
      val queryFoundFuzzy: Future[Seq[Matches]] = {
        val nameStringsQueries = foundFuzzyMatches.flatMap { case (cnp, cands) =>
          cands.filter { case (u, _) => u != NameStrings.emptyCanonicalUuid }
               .map { case (uuid, matchType) =>
                  val ns = nameStrings.filter { ns => ns.canonicalUuid === uuid }
                  val params = parameters.copy(query = cnp.result.input.verbatim.some,
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
        val matchedResult = matched.map { case ((cnp, cands), mtchs) =>
          mtchs.copy(matches = mtchs.matches, suppliedIdProvided = cnp.suppliedId,
                     scientificName = cnp.result.some)
        }
        val matchedFuzzyResult = {
          val unmatchedParts = unmatched.map { case ((cnp, _), _) =>
             cnp.copy(parts = cnp.parts.dropRight(1))
          }
          fuzzyMatch(unmatchedParts, parameters)
        }
        matchedFuzzyResult.map { mfr => matchedResult ++ mfr }
      }

      for (ufr <- unfoundFuzzyResult; ffr <- foundFuzzyResult) yield ufr ++ ffr ++ emptyMatches
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
      nwc <- resolveWildcard(namesWc.map { case (ns, _) => ns }, parameters)
      nex <- resolveExact(namesExact.map { case (ns, _) => ns }, parameters)
    } yield nwc ++ nex
  }

  private def resolveWildcard(names: Seq[NameRequest],
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
  def resolveExact(names: Seq[NameRequest], parameters: Parameters): Future[Seq[Matches]] = {
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
        val (matched, unmatchedAll) = exactMatches.zip(scientificNamesIds).partition {
          case (m, _) => m.matches.nonEmpty
        }
        val (unmatched, unmatchedNotParsed) = unmatchedAll.partition {
          case (_, (sn, _)) => sn.canonized().isDefined
        }
        val fuzzyMatches = {
          val parts = unmatched.map { case (_, (sn, localId)) =>
            CanonicalNameSplit(sn, sn.canonized().orZero.split(' ').toList, localId)
          }
          fuzzyMatch(parts, parameters)
        }

        val matchesResult = (matched ++ unmatchedNotParsed).map { case (mtch, (sn, suppliedId)) =>
          val ms = mtch.matches.map { m =>
            val mt: MatchType =
              if (m.nameString.name.id == sn.input.id) MatchType.ExactNameMatchByUUID
              else MatchType.ExactCanonicalNameMatchByUUID
            m.copy(matchType = mt)
          }
          mtch.copy(matches = ms, suppliedIdProvided = suppliedId, scientificName = sn.some)
        }

        fuzzyMatches.map { fm => fm ++ matchesResult }
      }
    }
  }
}

object Resolver {
  case class NameRequest(value: String, suppliedId: Option[SuppliedId])
}
