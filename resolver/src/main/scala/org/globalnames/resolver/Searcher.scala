package org.globalnames
package resolver

import org.apache.commons.lang3.StringUtils.capitalize
import model.Matches
import Searcher._

import scala.concurrent.Future

case class Searcher(resolver: Resolver, facetedSearcher: FacetedSearcher) {
  private def valueCleaned(value: String, modifier: Modifier): String = {
    val trimmed = value.replaceAll("\\s{2,}", " ").replaceAll("\\%", " ").trim
    modifier match {
      case UninomialModifier => capitalize(trimmed)
      case _ => trimmed
    }
  }

  def resolve(value: String, modifier: Modifier, wildcard: Boolean = false,
              take: Int = 50, drop: Int = 0): Future[Matches] = {
    val takeActual = take.min(50).max(0)
    val dropActual = drop.max(0)
    val resolverFun: (String, Int, Int) => Future[Matches] = modifier match {
      case NoModifier => resolver.resolveString
      case ExactModifier => facetedSearcher.resolveExact
      case NameStringModifier =>
        if (wildcard) facetedSearcher.resolveNameStringsLike
        else facetedSearcher.resolveNameStrings
      case CanonicalModifier =>
        if (wildcard) facetedSearcher.resolveCanonicalLike
        else facetedSearcher.resolveCanonical
      case UninomialModifier => facetedSearcher.resolveUninomial
      case GenusModifier => facetedSearcher.resolveGenus
      case SpeciesModifier => facetedSearcher.resolveSpecies
      case SubspeciesModifier => facetedSearcher.resolveSubspecies
      case AuthorModifier => facetedSearcher.resolveAuthor
      case YearModifier => facetedSearcher.resolveYear
    }
    resolverFun(valueCleaned(value, modifier), takeActual, dropActual)
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
