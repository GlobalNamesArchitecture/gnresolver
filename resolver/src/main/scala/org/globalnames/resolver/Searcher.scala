package org.globalnames
package resolver

import org.apache.commons.lang3.StringUtils.capitalize
import model._
import Searcher._

import slick.driver.PostgresDriver.api._

import scala.concurrent.Future

case class Searcher(db: Database, resolver: Resolver, facetedSearcher: FacetedSearcher)
  extends Materializer {
  import Materializer.Parameters

  private def valueCleaned(value: String, modifier: Modifier): String = {
    val trimmed = value.replaceAll("\\s{2,}", " ").replaceAll("\\%", " ").trim
    modifier match {
      case CanonicalModifier if !trimmed.startsWith("x ") => capitalize(trimmed)
      case NameStringModifier => capitalize(trimmed)
      case _ => trimmed
    }
  }

  private def resolverFunction(modifier: Modifier, wildcard: Boolean):
      (String) => Query[NameStrings, NameString, Seq] = modifier match {
      case ExactModifier => facetedSearcher.resolveExact
      case NameStringModifier =>
        if (wildcard) facetedSearcher.resolveNameStringsLike
        else facetedSearcher.resolveNameStrings
      case CanonicalModifier =>
        if (wildcard) facetedSearcher.resolveCanonicalLike
        else facetedSearcher.resolveCanonical
      case UninomialModifier =>
        if (wildcard) facetedSearcher.resolveUninomialWildcard
        else facetedSearcher.resolveUninomial
      case GenusModifier => facetedSearcher.resolveGenus
      case SpeciesModifier => facetedSearcher.resolveSpecies
      case SubspeciesModifier => facetedSearcher.resolveSubspecies
      case AuthorModifier => facetedSearcher.resolveAuthor
      case YearModifier => facetedSearcher.resolveYear
    }

  def resolve(value: String, modifier: Modifier, wildcard: Boolean = false,
              parameters: Parameters): Future[Matches] = {
    modifier match {
      case NoModifier =>
        resolver.resolveString(valueCleaned(value, modifier), parameters, wildcard)
      case _ =>
        val nameStrings = resolverFunction(modifier, wildcard)(valueCleaned(value, modifier))
        nameStringsMatches(nameStrings, parameters)
    }
  }
}

object Searcher {
  sealed trait Modifier
  case object NoModifier extends Modifier
  case object ExactModifier extends Modifier
  case object NameStringModifier extends Modifier
  case object CanonicalModifier extends Modifier
  case object UninomialModifier extends Modifier
  case object GenusModifier extends Modifier
  case object SpeciesModifier extends Modifier
  case object SubspeciesModifier extends Modifier
  case object AuthorModifier extends Modifier
  case object YearModifier extends Modifier
}
