package org.globalnames
package resolver

import org.apache.commons.lang3.StringUtils.capitalize
import model._
import Searcher._

import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Searcher(db: Database, resolver: Resolver, facetedSearcher: FacetedSearcher) {
  protected val nameStringIndicies = TableQuery[NameStringIndices]
  protected val dataSources = TableQuery[DataSources]

  private def valueCleaned(value: String, modifier: Modifier): String = {
    val trimmed = value.replaceAll("\\s{2,}", " ").replaceAll("\\%", " ").trim
    modifier match {
      case UninomialModifier => capitalize(trimmed)
      case _ => trimmed
    }
  }

  private def resolverFunction(modifier: Modifier, wildcard: Boolean):
      (String) => Query[NameStrings, NameString, Seq] =
    modifier match {
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

  def resolve(value: String, modifier: Modifier, wildcard: Boolean = false,
              take: Int = 50, drop: Int = 0): Future[Matches] = {
    val takeActual = take.min(50).max(0)
    val dropActual = drop.max(0)

    modifier match {
      case NoModifier =>
        resolver.resolveString(valueCleaned(value, modifier), takeActual, dropActual)
      case _ =>
        val resolverFun = resolverFunction(modifier, wildcard)
        val query = for {
          ns <- resolverFun(valueCleaned(value, modifier)).drop(dropActual).take(takeActual)
          nsi <- nameStringIndicies.filter { nsi => nsi.nameStringId === ns.id }
          ds <- dataSources.filter { ds => ds.id === nsi.dataSourceId }
        } yield (ns, nsi, ds)
        val queryCount = query.size
        for {
          portion <- db.run(query.result)
          count <- db.run(queryCount.result)
        } yield Matches(count,
                        portion.map { case (ns, nsi, ds) => Match(ns, ds, nsi) },
                        value)
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
