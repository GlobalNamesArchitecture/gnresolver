package org.globalnames
package resolver

import org.apache.commons.lang3.StringUtils
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
  protected val vernacularStrings = TableQuery[VernacularStrings]
  protected val vernacularStringIndices = TableQuery[VernacularStringIndices]

  private def wrapNameStringQuery(nameStringsQuery: Query[NameStrings, NameString, Seq],
                                  parameters: Parameters) = {
    val nameStringsQuerySurrogates =
      if (parameters.withSurrogates) nameStringsQuery
      else nameStringsQuery.filter { ns => ns.surrogate.isEmpty || !ns.surrogate }
    val query = for {
      ns <- nameStringsQuerySurrogates
      nsi <- nameStringIndicies.filter { nsi => nsi.nameStringId === ns.id }
      ds <- dataSources.filter { ds => ds.id === nsi.dataSourceId }
    } yield (ns, nsi, ds)
    val queryCount = query.size
    (query.drop(parameters.drop).take(parameters.take), queryCount)
  }

  private def vernacularsGet(portion: Seq[(NameString, NameStringIndex, DataSource)],
                             parameters: Parameters) = {
    val vernaculars =
      if (parameters.withVernaculars) {
        val qrys = portion.map { case (ns, nsi, ds) =>
          val qry = for {
            vsi <- vernacularStringIndices.filter { vsi =>
              vsi.dataSourceId === nsi.dataSourceId && vsi.taxonId === nsi.taxonId
            }
            vs <- vernacularStrings.filter { vs => vs.id === vsi.vernacularStringId }
          } yield (vs, vsi)
          qry.result
        }
        db.run(DBIO.sequence(qrys))
      } else {
        Future.successful(Seq.fill(portion.size)(Seq()))
      }
    vernaculars
  }

  protected def nameStringsMatches(
        nameStringsQuery: Query[NameStrings, NameString, Seq],
        parameters: Parameters): Future[Matches] = {
    val (query, queryCount) = wrapNameStringQuery(nameStringsQuery, parameters)
    for {
      portion <- db.run(query.result)
      count <- db.run(queryCount.result)
      vernaculars <- vernacularsGet(portion, parameters)
    } yield {
      Matches(count,
        portion.zip(vernaculars).map { case ((ns, nsi, ds), vs) =>
          val nameType = ns.canonicalName.map { n => StringUtils.countMatches(n.value, " ") + 1 }
          Match(ns, ds, nsi, vs, nameType, parameters.matchType)
        },
        suppliedInput = parameters.query)
    }
  }

  protected def nameStringsSequenceMatches(
        nameStringsQueries: Seq[(Query[NameStrings, NameString, Seq], Parameters)]):
          Future[Seq[Matches]] = {
    val (queryCount, query) = nameStringsQueries.map { case (nsq, parameters) =>
      val (res, count) = wrapNameStringQuery(nsq, parameters)
      (count.result, res.drop(parameters.drop).take(parameters.take).result)
    }.unzip
    for {
      portions <- db.run(DBIO.sequence(query))
      counts <- db.run(DBIO.sequence(queryCount))
      vernacular <- Future.sequence((portions, counts, nameStringsQueries.map { _._2 }).zipped.map {
        case (portion, count, parameters) =>
          val l = vernacularsGet(portion, parameters).map { vss =>
            Matches(count,
              portion.zip(vss).map { case ((ns, nsi, ds), vs) =>
                val nameType =
                  ns.canonicalName.map { n => StringUtils.countMatches(n.value, " ") + 1}
                Match(ns, ds, nsi, vs, nameType, parameters.matchType)
              },
              suppliedId = parameters.suppliedId,
              suppliedInput = parameters.query)
          }
          l
      })
    } yield {
      vernacular
    }
  }
}

object Materializer {
  case class Parameters(page: Int, perPage: Int,
                        withSurrogates: Boolean, withVernaculars: Boolean,
                        query: Option[String] = None,
                        suppliedId: Option[SuppliedId] = None,
                        matchType: MatchType = MatchType.Unknown) {
    val take: Int = perPage.min(1000).max(0)
    val drop: Int = (page * perPage).max(0)
  }
}
