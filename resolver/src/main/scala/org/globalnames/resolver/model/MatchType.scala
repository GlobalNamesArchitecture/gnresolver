package org.globalnames
package resolver
package model

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
