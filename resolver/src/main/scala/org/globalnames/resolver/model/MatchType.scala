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
  case object Fuzzy extends MatchType
  case object ExactMatchPartialByGenus extends MatchType
  case object Unknown extends MatchType

  def editDistance(matchType: MatchType): Int = matchType match {
    case Fuzzy => 1
    case _ => 0
  }

  def score(matchType: MatchType): Int = matchType match {
    case UUIDLookup => 1
    case ExactNameMatchByUUID => 2
    case ExactNameMatchByString => 3
    case ExactCanonicalNameMatchByUUID => 4
    case ExactCanonicalNameMatchByString => 5
    case Fuzzy => 6
    case ExactMatchPartialByGenus => 7
    case Unknown => 8
  }
}
