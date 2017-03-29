package org.globalnames
package resolver

import parser.ScientificNameParser.{Result => SNResult}

import scalaz._
import Scalaz._

package object model {

  sealed trait MatchType
  object MatchType {
    case object Unknown extends MatchType
    case object ExactNameMatchByUUID extends MatchType
    case object ExactNameMatchByString extends MatchType
    case object ExactCanonicalNameMatchByUUID extends MatchType
    case object ExactCanonicalNameMatchByString extends MatchType
    case object Fuzzy extends MatchType
    case object ExactMatchPartialByGenus extends MatchType
    case object UUIDLookup extends MatchType
  }

  type SuppliedId = String

  case class Match(nameString: NameString, dataSource: DataSource, nameStringIndex: NameStringIndex,
                   vernacularStrings: Seq[(VernacularString, VernacularStringIndex)],
                   nameType: Option[Int], matchType: MatchType = MatchType.Unknown)

  case class Matches(total: Long, matches: Seq[Match],
                     suppliedInput: Option[String] = None,
                     private val suppliedIdProvided: Option[SuppliedId] = None,
                     scientificName: Option[SNResult] = None) {
    val suppliedId: Option[SuppliedId] = suppliedIdProvided.map { _.trim }
  }

  case class AuthorScore(authorshipInput: String, authorshipMatch: String, value: Double)
  case class Score(matchType: MatchType, nameType: Option[Int], authorScore: AuthorScore,
                   parsingQuality: Int)

  case class MatchScored(mtch: Match, score: Score)

  object Matches {
    def empty: Matches = Matches(0, Seq(), None)

    def empty(suppliedInput: String): Matches = Matches(0, Seq(), suppliedInput.some)
  }

}
