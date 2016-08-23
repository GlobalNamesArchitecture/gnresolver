package org.globalnames.resolver.api

import scala.util.parsing.combinator.RegexParsers

object QueryParser extends RegexParsers {
  case class Modifier(typ: String)
  case class SearchPart(modifier: Modifier, contents: String, wildcard: Boolean)

  val noModifier = "none"
  val exactModifier = "exact"
  val nameStringModifier = "ns"
  val canonicalModifier = "can"
  val uninomialModifier = "uni"
  val genusModifier = "gen"
  val speciesModifier = "sp"
  val subspeciesModifier = "ssp"
  val authorModifier = "au"
  val yearModifier = "yr"

  private def word = """[A-Za-z0-9]+""".r

  private def modifier: Parser[Modifier] =
    (exactModifier | nameStringModifier | canonicalModifier |
     uninomialModifier | genusModifier | speciesModifier |
     subspeciesModifier | authorModifier | yearModifier) ^^ Modifier

  private def wildcard: String = "*"

  private def searchPartModifier: Parser[SearchPart] =
    modifier ~ ":" ~ rep1(not(modifier) ~> word) ~
    opt(wildcard) ^^ { case mod ~ _ ~ parts ~ wildcard =>
      SearchPart(mod, parts.mkString(" "), wildcard.isDefined)
    }

  private def searchPart =
    searchPartModifier |
      (s"""[^$wildcard]*""".r ~ opt(wildcard) ^^ { case contents ~ wildcard =>
        SearchPart(Modifier("none"), contents, wildcard.isDefined)
      })

  private def searchQuery =
    rep1(searchPart)

  def result(text: String): SearchPart = parse(searchPart, text) match {
    case Success(sp, _) => sp
  }
}