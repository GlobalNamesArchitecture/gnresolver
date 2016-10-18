package org.globalnames
package resolver
package api

import scala.util.parsing.combinator.RegexParsers
import Searcher._

object QueryParser extends RegexParsers {
  case class SearchPart(modifier: Modifier, contents: String, wildcard: Boolean)

  private val exactModifier = "exact" ^^ { _ => ExactModifier }
  private val nameStringModifier = "ns" ^^ { _ => NameStringModifier }
  private val canonicalModifier = "can" ^^ { _ => CanonicalModifier }
  private val uninomialModifier = "uni" ^^ { _ => UninomialModifier }
  private val genusModifier = "gen" ^^ { _ => GenusModifier }
  private val speciesModifier = "sp" ^^ { _ => SpeciesModifier }
  private val subspeciesModifier = "ssp" ^^ { _ => SubspeciesModifier }
  private val authorModifier = "au" ^^ { _ => AuthorModifier }
  private val yearModifier = "yr" ^^ { _ => YearModifier }

  private def word = """[A-Za-z0-9]+""".r

  private def modifier: Parser[Modifier] =
    exactModifier | nameStringModifier | canonicalModifier |
    uninomialModifier | genusModifier | speciesModifier |
    subspeciesModifier | authorModifier | yearModifier

  private def wildcard: String = "*"

  private def searchPartModifier: Parser[SearchPart] =
    modifier ~ ":" ~ rep1(not(modifier) ~> word) ~
    opt(wildcard) ^^ { case mod ~ _ ~ parts ~ wildcard =>
      SearchPart(mod, parts.mkString(" "), wildcard.isDefined)
    }

  private def searchPartNoModifier: Parser[SearchPart] =
    s"""[^$wildcard]*""".r ~ opt(wildcard) ^^ { case contents ~ wildcard =>
      SearchPart(NoModifier, contents, wildcard.isDefined)
    }

  private def searchPart = searchPartModifier | searchPartNoModifier


  private def searchQuery = rep1(searchPart)

  def result(text: String): SearchPart = parse(searchPart, text) match {
    case Success(sp, _) => sp
  }
}
