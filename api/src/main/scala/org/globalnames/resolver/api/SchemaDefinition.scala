package org.globalnames
package resolver
package api

import model.{AuthorScore, Matches, Score}
import model.db.{DataSource, Name, NameStringIndex, VernacularString, VernacularStringIndex}
import Materializer.Parameters
import sangria.schema._
import sangria.marshalling.sprayJson._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import java.util.UUID

import org.globalnames.resolver.Resolver.NameRequest

trait SchemaDefinition extends DefaultJsonProtocol with CrossMapProtocols {
  val nameStringsMaxCount = 1000
  implicit val nameRequestFormat: RootJsonFormat[NameRequest] =
    jsonFormat2(Resolver.NameRequest.apply)

  val DataSource = ObjectType(
    "DataSource", fields[Unit, DataSource](
        Field("id", IntType, resolve = _.value.id)
      , Field("title", StringType, resolve = _.value.title)
    )
  )

  val Name = ObjectType(
    "Name", fields[Unit, Name](
        Field("id", IDType, resolve = _.value.id.toString)
      , Field("name", StringType, resolve = _.value.value)
    )
  )

  val MatchType = ObjectType(
    "MatchType", fields[Unit, model.MatchType](
        Field("value", StringType, resolve = _.value.toString)
      , Field("score", IntType, resolve = ctx => model.MatchType.score(ctx.value))
      , Field("editDistance", IntType, resolve = ctx => model.MatchType.editDistance(ctx.value))
    )
  )

  val Classification = ObjectType(
    "Classification", fields[Unit, NameStringIndex](
        Field("path", OptionType(StringType), resolve = _.value.classificationPath)
      , Field("pathIds", OptionType(IDType), resolve = _.value.classificationPathIds)
      , Field("pathRanks", OptionType(StringType), resolve = _.value.classificationPathRanks)
    )
  )

  val Vernacular = ObjectType(
    "Vernacular", fields[Unit, (VernacularString, VernacularStringIndex)](
        Field("id", IDType, resolve = _.value._1.id.toString)
      , Field("name", StringType, resolve = _.value._1.name)
      , Field("dataSourceId", IntType, resolve = _.value._2.dataSourceId)
    )
  )

  val AuthorScoreObj = ObjectType(
    "AuthorScore", fields[Unit, AuthorScore](
        Field("authorshipInput", StringType, resolve = _.value.authorshipInput)
      , Field("authorshipMatch", StringType, resolve = _.value.authorshipMatch)
      , Field("value", FloatType, resolve = _.value.value)
    )
  )

  val ScoreObj = ObjectType(
    "Score", fields[Unit, Score](
        Field("matchType", MatchType, resolve = _.value.matchType)
      , Field("nameType", OptionType(IntType), resolve = _.value.nameType)
      , Field("authorScore", AuthorScoreObj, resolve = _.value.authorScore)
      , Field("parsingQuality", IntType, resolve = _.value.parsingQuality)
    )
  )

  val ResponseItem = ObjectType(
    "ResponseItem", fields[GnRepo, model.MatchScored](
        Field("name", Name, resolve = _.value.mtch.nameString.name)
      , Field("canonicalName", OptionType(Name), resolve = _.value.mtch.nameString.canonicalName)
      , Field("surrogate", OptionType(BooleanType), resolve = _.value.mtch.nameString.surrogate)
      , Field("synonym", OptionType(BooleanType), resolve = _.value.mtch.synonym)
      , Field("dataSource", OptionType(DataSource), resolve = _.value.mtch.dataSource)
      , Field("taxonId", IDType, resolve = _.value.mtch.nameStringIndex.taxonId)
      , Field("globalId", OptionType(IDType), resolve = _.value.mtch.nameStringIndex.globalId)
      , Field("localId", OptionType(IDType), resolve = _.value.mtch.nameStringIndex.localId)
      , Field("classification", OptionType(Classification), resolve = _.value.mtch.nameStringIndex)
      , Field("vernaculars", ListType(Vernacular), resolve = _.value.mtch.vernacularStrings)
      , Field("matchType", MatchType, resolve = _.value.mtch.matchType)
      , Field("preScore", ScoreObj, resolve = _.value.score)
    )
  )

  val Response = ObjectType(
    "Response", fields[GnRepo, model.Matches](
        Field("total", LongType, None, resolve = _.value.total)
      , Field("suppliedInput", OptionType(StringType), None, resolve = _.value.suppliedInput)
      , Field("suppliedId", OptionType(StringType), None, resolve = _.value.suppliedId)
      , Field("results", ListType(ResponseItem), None, resolve = ctx => Scores.compute(ctx.value))
    )
  )

  object CrossMap {
    val Source = ObjectType(
      "Source", fields[Unit, CrossMapSearcher.Source](
          Field("dbId", IntType, resolve = _.value.dbId)
        , Field("localId", StringType, resolve = _.value.localId)
      )
    )

    val Target = ObjectType(
      "Target", fields[Unit, CrossMapSearcher.Target](
          Field("dbSinkId", IntType, resolve = _.value.dbSinkId)
        , Field("dbTargetId", IntType, resolve = _.value.dbTargetId)
        , Field("localId", StringType, resolve = _.value.localId)
      )
    )

    val Result = ObjectType(
      "Result", fields[Unit, CrossMapSearcher.Result](
          Field("source", Source, resolve = _.value.source)
        , Field("target", ListType(Target), resolve = _.value.target)
      )
    )

    val Sinks = Argument("sinks", InputObjectType[CrossMapRequest](
      "CrossMapSink", List(
          InputField("dbSinkIds", ListInputType(IntType),
                     description = "Datasources to apply cross-map through")
        , InputField("localIds", ListInputType(StringType),
                     description = "Local IDs in `DBSourceId` to apply cross-map against")
      )
    ))
    val DBSourceId = Argument("dataSourceId", IntType,
                              description = "The database to cross-map data from")
    val DBTargetId = Argument("dataTargetId", OptionInputType(IntType),
                              description = "The database to cross-map data to")
  }

  val Id = Argument("id", StringType)
  val Page = Argument("page", OptionInputType(IntType), 0)
  val PerPage = Argument("perPage", OptionInputType(IntType), nameStringsMaxCount)
  val WithSurrogates = Argument("withSurrogates", OptionInputType(BooleanType), false)
  val WithVernaculars = Argument("withVernaculars", OptionInputType(BooleanType), false)
  val NameRequestObj: InputObjectType[NameRequest] = InputObjectType[NameRequest]("name", List(
      InputField("value", StringType)
    , InputField("suppliedId", OptionInputType(StringType))
  ))
  val NamesRequest = Argument("names", ListInputType(NameRequestObj))
  val DataSourceIds = Argument("dataSourceIds", OptionInputType(ListInputType(IntType)))
  val SearchTerm = Argument("searchTerm", StringType)

  val QueryType = ObjectType(
    "Query", fields[GnRepo, Unit](
        Field("nameString", Response,
          arguments = List(Id, Page, PerPage, WithSurrogates, WithVernaculars),
          resolve = ctx => ctx.withArgs(Id, Page, PerPage, WithVernaculars) {
            (id, page, perPage, withVernaculars) =>
              ctx.ctx.nameStringByUuid(id, page, perPage, withVernaculars)
          }
        )
      , Field("nameResolvers", ListType(Response),
          arguments = List(NamesRequest, DataSourceIds, Page, PerPage, WithSurrogates,
                           WithVernaculars),
          resolve = ctx => ctx.withArgs(NamesRequest, DataSourceIds, Page, PerPage,
                                        WithSurrogates, WithVernaculars) {
            (names, dataSourceIdsOpt, page, perPage, withSurrogates, withVernaculars) =>
              val dataSourceIds = dataSourceIdsOpt.map { _.toVector }.getOrElse(Vector())
              ctx.ctx.nameStrings(names.take(nameStringsMaxCount), dataSourceIds, page, perPage,
                                  withSurrogates, withVernaculars)
          }
        )
      , Field("nameStrings", Response,
          arguments = List(SearchTerm, Page, PerPage, WithSurrogates, WithVernaculars),
          resolve = ctx => ctx.withArgs(SearchTerm, Page, PerPage, WithSurrogates,
                                        WithVernaculars) { ctx.ctx.nameResolve })
      , Field("crossMap", ListType(CrossMap.Result),
          arguments = List(CrossMap.DBSourceId, CrossMap.DBTargetId, CrossMap.Sinks),
          resolve = ctx => ctx.withArgs(CrossMap.DBSourceId, CrossMap.DBTargetId, CrossMap.Sinks) {
            ctx.ctx.crossMapResolve
          })
    )
  )

  val schema = Schema(QueryType)

  case class GnRepo(facetedSearcher: FacetedSearcher, resolver: Resolver,
                    searcher: Searcher, crossMap: CrossMapSearcher)
                   (implicit executor: ExecutionContextExecutor) {
    def nameStringByUuid(uuid: String, page: Int, perPage: Int,
                         withVernaculars: Boolean): Future[Matches] = {
      val uuidParsed = UUID.fromString(uuid)
      val params = Parameters(page, perPage, withSurrogates = false, withVernaculars)
      facetedSearcher.findNameStringByUuid(uuidParsed, params)
    }

    def nameStrings(names: Seq[NameRequest], dataSourceIds: Vector[Int],
                    page: Int, perPage: Int,
                    withSurrogates: Boolean, withVernaculars: Boolean): Future[Seq[Matches]] = {
      val params = Parameters(page, perPage, withSurrogates, withVernaculars,
                              dataSourceIds = dataSourceIds)
      resolver.resolveExact(names, params)
    }

    def nameResolve(searchTerm: String, page: Int, perPage: Int,
                    withSurrogates: Boolean, withVernaculars: Boolean): Future[Matches] = {
      val search = QueryParser.result(searchTerm)
      val params = Parameters(page, perPage, withSurrogates, withVernaculars)
      searcher.resolve(search.contents, search.modifier, search.wildcard, params)
    }

    def crossMapResolve(dbSourceId: Int, dbTargetId: Option[Int],
                        crossMapReq: CrossMapRequest): Future[Seq[CrossMapSearcher.Result]] = {
      crossMap.execute(dbSourceId, crossMapReq.dbSinkIds, dbTargetId, crossMapReq.localIds)
    }
  }
}
