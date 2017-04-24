package org.globalnames
package resolver
package model

sealed trait MatchType

object MatchType {
  case object UUIDLookup extends MatchType
  case object ExactNameMatchByUUID extends MatchType
  case object ExactNameMatchByString extends MatchType
  case object ExactCanonicalNameMatchByUUID extends MatchType
  case object ExactCanonicalNameMatchByString extends MatchType
  case object FuzzyCanonicalMatch extends MatchType
  case object FuzzyPartialMatch extends MatchType
  case object ExactMatchPartialByGenus extends MatchType
  case object ExactPartialMatch extends MatchType
  case object Unknown extends MatchType

  def editDistance(matchType: MatchType): Int = matchType match {
    case FuzzyPartialMatch => 1
    case _ => 0
  }

  def score(matchType: MatchType): Int = matchType match {
    case UUIDLookup => 1
    case ExactNameMatchByUUID => 2
    case ExactNameMatchByString => 3
    case ExactCanonicalNameMatchByUUID => 4
    case ExactCanonicalNameMatchByString => 5
    case FuzzyCanonicalMatch => 6
    case ExactPartialMatch => 7
    case FuzzyPartialMatch => 8
    case ExactMatchPartialByGenus => 9
    case Unknown => 10
  }
}
