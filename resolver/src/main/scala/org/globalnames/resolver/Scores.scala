package org.globalnames
package resolver

import parser.ScientificNameParser.{instance => snp}
import model.{AuthorScore, Matches, MatchScored, Score}

import scalaz._
import Scalaz._
import scala.util.Try

object Scores {
  def compute(matches: Matches): Seq[MatchScored] = {
    val authorshipInput = matches.scientificName.map { x =>
      x.authorshipNames.map { asn => Author(asn.mkString(" ")) }
    }.getOrElse(Seq())
    val yearInput = for { sn <- matches.scientificName
                          yrStr <- sn.yearDelimited
                          yr <- Try(yrStr.toInt).toOption } yield yr
    for (mtch <- matches.matches) yield {
      val parsedString = snp.fromString(mtch.nameString.name.value)
      val authorshipMatch = parsedString.authorshipNames.map { as => Author(as.mkString(" ")) }
      val yearMatch = for { yr <- parsedString.yearDelimited; y <- Try(yr.toInt).toOption }
                      yield y
      val score = AuthorsMatcher.score(authorshipInput, yearInput, authorshipMatch, yearMatch)
      val authScore = AuthorScore(
        s"${authorshipInput.map { _.name }.mkString(" | ")} || year: $yearInput",
        s"${authorshipMatch.map { _.name }.mkString(" | ")} || year: $yearMatch",
        score)
      val scr = Score(mtch.matchType, mtch.nameType, authScore, parsedString.scientificName.quality)
      MatchScored(mtch, scr)
    }
  }
}
