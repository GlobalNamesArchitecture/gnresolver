package org.globalnames.resolver.model

case class AuthorScore(authorshipInput: String, authorshipMatch: String, value: Double)

case class Score(matchType: MatchType, nameType: Option[Int], authorScore: AuthorScore,
                 parsingQuality: Int, value: Option[Double], message: Option[String])

case class MatchScored(mtch: Match, score: Score)
