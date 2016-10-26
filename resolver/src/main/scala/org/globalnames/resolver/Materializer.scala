package org.globalnames
package resolver

import resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Materializer {
  import Materializer.Parameters

  protected val db: Database
  protected val nameStringIndicies = TableQuery[NameStringIndices]
  protected val dataSources = TableQuery[DataSources]
  protected val nameStrings = TableQuery[NameStrings]
  protected val authorWords = TableQuery[AuthorWords]
  protected val uninomialWords = TableQuery[UninomialWords]
  protected val genusWords = TableQuery[GenusWords]
  protected val speciesWords = TableQuery[SpeciesWords]
  protected val subspeciesWords = TableQuery[SubspeciesWords]
  protected val yearWords = TableQuery[YearWords]

  private def wrapNameStringQuery(nameStringsQuery: Query[NameStrings, NameString, Seq],
                                  parameters: Parameters) = {
    val nameStringsQuery1 =
      if (parameters.withSurrogates) nameStringsQuery
      else nameStringsQuery.filter { !_.surrogate }
    val query = for {
      ns <- nameStringsQuery1
      nsi <- nameStringIndicies.filter { nsi => nsi.nameStringId === ns.id }
      ds <- dataSources.filter { ds => ds.id === nsi.dataSourceId }
    } yield (ns, nsi, ds)
    query
  }

  protected def nameStringsMatches(
        nameStringsQuery: Query[NameStrings, NameString, Seq],
        parameters: Parameters): Future[Matches] = {
    val query = wrapNameStringQuery(nameStringsQuery, parameters)
    val queryCount = query.countDistinct
    for {
      portion <- db.run(query.drop(parameters.dropActual).take(parameters.takeActual).result)
      count <- db.run(queryCount.result)
    } yield Matches(count,
                    portion.map { case (ns, nsi, ds) => Match(ns, ds, nsi) },
                    suppliedNameString = parameters.query)
  }

  protected def nameStringsSequenceMatches(
        nameStringsQueries: Seq[(Query[NameStrings, NameString, Seq], Parameters)]):
          Future[Seq[Matches]] = {
    val query = nameStringsQueries.map { case (nsq, parameters) =>
      wrapNameStringQuery(nsq, parameters).drop(parameters.dropActual)
                                          .take(parameters.takeActual).result
    }
    val queryCount = nameStringsQueries.map { case (nsq, parameters) =>
      wrapNameStringQuery(nsq, parameters).countDistinct.result
    }
    for {
      portions <- db.run(DBIO.sequence(query))
      counts <- db.run(DBIO.sequence(queryCount))
    } yield {
      (portions, counts, nameStringsQueries.map { _._2 }).zipped.map {
        case (portion, count, parameters) =>
          Matches(count,
                  portion.map { case (ns, nsi, ds) => Match(ns, ds, nsi, parameters.kind) },
                  localId = parameters.localId,
                  suppliedNameString = parameters.query)
      }
    }
  }
}

object Materializer {
  case class Parameters(take: Int, drop: Int, withSurrogates: Boolean, query: String = "",
                        localId: Option[LocalId] = None, kind: Kind = Kind.None) {
    val takeActual = take.min(1000).max(0)
    val dropActual = drop.max(0)
  }
}
