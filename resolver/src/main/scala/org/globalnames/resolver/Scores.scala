package org.globalnames
package resolver

import parser.ScientificNameParser.{instance => snp}
import model._
import org.globalnames.resolver.model.MatchType._

import scalaz._
import Scalaz._
import scala.util.Try

object Scores {
  private def sigmoid(x: Double) = 1 / (1 + math.exp(-x))

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

      val scoreMsg = mtch.nameType match {
        case Some(nt) =>
          val res = nt match {
            case 1 =>
              mtch.matchType match {
                case ExactNameMatchByUUID => (2.0, 4.0).right
                case ExactCanonicalNameMatchByUUID => (2.0, 1.0).right
                case FuzzyCanonicalMatch => (1.0, 0.0).right
                case _ => s"Unexpected type of match type ${mtch.matchType} for nameType $nt".left
              }
            case 2 =>
              mtch.matchType match {
                case ExactNameMatchByUUID => (2.0, 8.0).right
                case ExactCanonicalNameMatchByUUID => (2.0, 3.0).right
                case FuzzyCanonicalMatch | ExactMatchPartialByGenus => (1.0, 1.0).right
                case _ => s"Unexpected type of match type ${mtch.matchType} for nameType $nt".left
              }
            case _ =>
              mtch.matchType match {
                case ExactNameMatchByUUID => (2.0, 8.0).right
                case ExactCanonicalNameMatchByUUID => (2.0, 7.0).right
                case FuzzyCanonicalMatch | FuzzyPartialMatch | ExactMatchPartialByGenus =>
                  (1.0, 0.5).right
                case _ => s"Unexpected type of match type ${mtch.matchType} for nameType $nt".left
              }
          }
          res.rightMap { case (authorCoef, generalCoef) =>
            authScore.value * authorCoef + generalCoef
          }
        case None =>
          mtch.matchType match {
            case MatchType.ExactNameMatchByUUID => 0.5.right
            case _ => s"No match".left
          }
      }

      val scr = Score(mtch.matchType, mtch.nameType, authScore,
                      parsedString.scientificName.quality,
                      scoreMsg.rightMap { x => sigmoid(x) }.toOption,
                      scoreMsg.swap.toOption)
      MatchScored(mtch, scr)
    }
  }
}
