package org.globalnames
package resolver

import org.apache.commons.lang3.StringUtils.capitalize
import model._
import Searcher._
import model.db.{NameString, NameStrings}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

case class Searcher(database: Database, resolver: Resolver, facetedSearcher: FacetedSearcher)
  extends Materializer {
  import Materializer.Parameters

  private def valueCleaned(value: String, modifier: Modifier): String = {
    val trimmed = value.replaceAll("\\s{2,}", " ").replaceAll("\\%", " ").trim
    modifier match {
      case CanonicalModifier if !trimmed.startsWith("x ") => capitalize(trimmed)
      case NameStringModifier => capitalize(trimmed)
      case NoModifier => capitalize(trimmed)
      case _ => trimmed
    }
  }

  def resolve(value: String, modifier: Modifier, wildcard: Boolean = false,
              parameters: Parameters): Future[Matches] = {
    var paramsRes = parameters.copy(matchType = MatchType.ExactNameMatchByString)
    val resolverFunction: (String) => Query[NameStrings, NameString, Seq] = modifier match {
      case NoModifier =>
        if (wildcard) facetedSearcher.resolveWildcard
        else facetedSearcher.resolve
      case ExactModifier =>
        paramsRes = paramsRes.copy(matchType = MatchType.ExactNameMatchByUUID)
        facetedSearcher.resolveExact
      case NameStringModifier =>
        if (wildcard) facetedSearcher.resolveNameStringsLike
        else {
          paramsRes = paramsRes.copy(matchType = MatchType.ExactNameMatchByString)
          facetedSearcher.resolveNameStrings
        }
      case CanonicalModifier =>
        if (wildcard) {
          paramsRes = paramsRes.copy(matchType = MatchType.ExactCanonicalNameMatchByUUID)
          facetedSearcher.resolveCanonicalLike
        } else {
          paramsRes = paramsRes.copy(matchType = MatchType.ExactCanonicalNameMatchByString)
          facetedSearcher.resolveCanonical
        }
      case UninomialModifier =>
        if (wildcard) facetedSearcher.resolveUninomialWildcard
        else facetedSearcher.resolveUninomial
      case GenusModifier =>
        if (wildcard) facetedSearcher.resolveGenusWildcard
        else facetedSearcher.resolveGenus
      case SpeciesModifier =>
        if (wildcard) facetedSearcher.resolveSpeciesWildcard
        else facetedSearcher.resolveSpecies
      case SubspeciesModifier =>
        if (wildcard) facetedSearcher.resolveSubspeciesWildcard
        else facetedSearcher.resolveSubspecies
      case AuthorModifier =>
        if (wildcard) facetedSearcher.resolveAuthorWildcard
        else facetedSearcher.resolveAuthor
      case YearModifier =>
        if (wildcard) facetedSearcher.resolveYearWildcard
        else facetedSearcher.resolveYear
    }
    val nameStrings = resolverFunction(valueCleaned(value, modifier))
    nameStringsMatches(nameStrings, paramsRes)
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
