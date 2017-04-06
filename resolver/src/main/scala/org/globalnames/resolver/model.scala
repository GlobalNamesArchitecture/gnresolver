package org.globalnames
package resolver

import parser.ScientificNameParser.{Result => SNResult}

import scalaz._
import Scalaz._

package object model {

  sealed trait MatchType {
    val score: Int
  }
  object MatchType {
    case object UUIDLookup extends MatchType {
      val score: Int = 1
    }
    case object ExactNameMatchByUUID extends MatchType {
      val score: Int = 2
    }
    case object ExactNameMatchByString extends MatchType {
      val score: Int = 3
    }
    case object ExactCanonicalNameMatchByUUID extends MatchType {
      val score: Int = 4
    }
    case object ExactCanonicalNameMatchByString extends MatchType {
      val score: Int = 5
    }
    case object Fuzzy extends MatchType {
      val score: Int = 6
    }
    case object ExactMatchPartialByGenus extends MatchType {
      val score: Int = 7
    }
    case object Unknown extends MatchType {
      val score: Int = 8
    }

    def editDistance(matchType: MatchType): Int = matchType match {
      case Fuzzy => 1
      case _ => 0
    }
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
