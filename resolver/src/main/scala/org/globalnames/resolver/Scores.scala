package org.globalnames
package resolver

import parser.ScientificNameParser.{instance => snp}
import model.{AuthorScore, Matches, Score}
import parser.Authorship

import scalaz._
import Scalaz._
import scala.util.Try

object Scores {
  def compute(matches: Matches): Seq[Score] = {
    val authorshipInput = matches.scientificName.map { x =>
      x.authorshipNames.map { asn => Author(asn.mkString(" ")) }
    }.getOrElse(Seq())
    val yearInput = for { sn <- matches.scientificName
                          yrStr <- sn.yearDelimited
                          yr <- Try(yrStr.toInt).toOption } yield yr
    matches.matches.map { mtch =>
      val parsedString = snp.fromString(mtch.nameString.name.value)
      val authorshipMatch = parsedString.authorshipNames.map { as => Author(as.mkString(" ")) }
      val yearMatch = for { yr <- parsedString.yearDelimited
                            y <- Try(yr.toInt).toOption } yield y
      val score = AuthorsMatcher.score(authorshipInput, yearInput, authorshipMatch, yearMatch)
      val authScore =
        AuthorScore(authorshipInput.map { _.name }.mkString(" | ") + " || " + yearInput,
                    authorshipMatch.map { _.name }.mkString(" | ") + " || " + yearMatch,
                    score)
      Score(mtch.matchType, mtch.nameType, authScore, parsedString.scientificName.quality)
    }
  }
}
