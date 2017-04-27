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

  val DataSourceOT = ObjectType(
    "DataSource", fields[Unit, DataSource](
        Field("id", IntType, resolve = _.value.id)
      , Field("title", StringType, resolve = _.value.title)
    )
  )

  val NameOT = ObjectType(
    "Name", fields[Unit, Name](
        Field("id", IDType, resolve = _.value.id.toString)
      , Field("name", StringType, resolve = _.value.value)
    )
  )

  val MatchTypeOT = ObjectType(
    "MatchType", fields[Unit, model.MatchType](
        Field("value", StringType, resolve = _.value.toString)
      , Field("score", IntType, resolve = ctx => model.MatchType.score(ctx.value))
      , Field("editDistance", IntType, resolve = ctx => model.MatchType.editDistance(ctx.value))
    )
  )

  val ClassificationOT = ObjectType(
    "Classification", fields[Unit, NameStringIndex](
        Field("path", OptionType(StringType), resolve = _.value.classificationPath)
      , Field("pathIds", OptionType(IDType), resolve = _.value.classificationPathIds)
      , Field("pathRanks", OptionType(StringType), resolve = _.value.classificationPathRanks)
    )
  )

  val VernacularOT = ObjectType(
    "Vernacular", fields[Unit, (VernacularString, VernacularStringIndex)](
        Field("id", IDType, resolve = _.value._1.id.toString)
      , Field("name", StringType, resolve = _.value._1.name)
      , Field("dataSourceId", IntType, resolve = _.value._2.dataSourceId)
    )
  )

  val AuthorScoreOT = ObjectType(
    "AuthorScore", fields[Unit, AuthorScore](
        Field("authorshipInput", StringType, resolve = _.value.authorshipInput)
      , Field("authorshipMatch", StringType, resolve = _.value.authorshipMatch)
      , Field("value", FloatType, resolve = _.value.value)
    )
  )

  val ScoreOT = ObjectType(
    "Score", fields[Unit, Score](
        Field("matchType", MatchTypeOT, resolve = _.value.matchType)
      , Field("nameType", OptionType(IntType), resolve = _.value.nameType)
      , Field("authorScore", AuthorScoreOT, resolve = _.value.authorScore)
      , Field("parsingQuality", IntType, resolve = _.value.parsingQuality)
      , Field("value", OptionType(FloatType), resolve = _.value.value)
      , Field("message", OptionType(StringType), resolve = _.value.message)
    )
  )

  val ResponseItemOT = ObjectType(
    "ResponseItem", fields[GnRepo, model.MatchScored](
        Field("name", NameOT, resolve = _.value.mtch.nameString.name)
      , Field("canonicalName", OptionType(NameOT), resolve = _.value.mtch.nameString.canonicalName)
      , Field("surrogate", OptionType(BooleanType), resolve = _.value.mtch.nameString.surrogate)
      , Field("synonym", OptionType(BooleanType), resolve = _.value.mtch.synonym)
      , Field("dataSource", OptionType(DataSourceOT), resolve = _.value.mtch.dataSource)
      , Field("taxonId", IDType, resolve = _.value.mtch.nameStringIndex.taxonId)
      , Field("globalId", OptionType(IDType), resolve = _.value.mtch.nameStringIndex.globalId)
      , Field("localId", OptionType(IDType), resolve = _.value.mtch.nameStringIndex.localId)
      , Field("classification", OptionType(ClassificationOT),
              resolve = _.value.mtch.nameStringIndex)
      , Field("acceptedTaxonId", OptionType(StringType),
              resolve = _.value.mtch.nameStringIndex.acceptedTaxonId)
      , Field("acceptedName", OptionType(NameOT),
              resolve = _.value.mtch.nameStringIndex.acceptedName)
      , Field("vernaculars", ListType(VernacularOT), resolve = _.value.mtch.vernacularStrings)
      , Field("matchType", MatchTypeOT, resolve = _.value.mtch.matchType)
      , Field("score", ScoreOT, resolve = _.value.score)
    )
  )

  val ResponseOT = ObjectType(
    "Response", fields[GnRepo, model.Matches](
        Field("total", LongType, None, resolve = _.value.total)
      , Field("suppliedInput", OptionType(StringType), None, resolve = _.value.suppliedInput)
      , Field("suppliedId", OptionType(StringType), None, resolve = _.value.suppliedId)
      , Field("results", ListType(ResponseItemOT), None, resolve = ctx => Scores.compute(ctx.value))
    )
  )

  object CrossMap {
    val SourceOT = ObjectType(
      "Source", fields[Unit, CrossMapSearcher.Source](
          Field("dbId", IntType, resolve = _.value.dbId)
        , Field("localId", StringType, resolve = _.value.localId)
      )
    )

    val TargetOT = ObjectType(
      "Target", fields[Unit, CrossMapSearcher.Target](
          Field("dbSinkId", IntType, resolve = _.value.dbSinkId)
        , Field("dbTargetId", IntType, resolve = _.value.dbTargetId)
        , Field("localId", StringType, resolve = _.value.localId)
      )
    )

    val ResultOT = ObjectType(
      "Result", fields[Unit, CrossMapSearcher.Result](
          Field("source", SourceOT, resolve = _.value.source)
        , Field("target", ListType(TargetOT), resolve = _.value.target)
      )
    )

    val SinksArg = Argument("sinks", InputObjectType[CrossMapRequest](
      "CrossMapSink", List(
          InputField("dbSinkIds", ListInputType(IntType),
                     description = "Datasources to apply cross-map through")
        , InputField("localIds", ListInputType(StringType),
                     description = "Local IDs in `DBSourceId` to apply cross-map against")
      )
    ))
    val DBSourceIdArg = Argument("dataSourceId", IntType,
                                 description = "The database to cross-map data from")
    val DBTargetIdArg = Argument("dataTargetId", OptionInputType(IntType),
                                 description = "The database to cross-map data to")
  }

  val IdArg = Argument("id", StringType)
  val PageArg = Argument("page", OptionInputType(IntType), 0)
  val PerPageArg = Argument("perPage", OptionInputType(IntType), nameStringsMaxCount)
  val WithSurrogatesArg = Argument("withSurrogates", OptionInputType(BooleanType), false)
  val WithVernacularsArg = Argument("withVernaculars", OptionInputType(BooleanType), false)
  val NameRequestIOT: InputObjectType[NameRequest] = InputObjectType[NameRequest]("name", List(
      InputField("value", StringType)
    , InputField("suppliedId", OptionInputType(StringType))
  ))
  val NamesRequestArg = Argument("names", ListInputType(NameRequestIOT))
  val DataSourceIdsArg = Argument("dataSourceIds", OptionInputType(ListInputType(IntType)))
  val SearchTermArg = Argument("searchTerm", StringType)

  val QueryTypeOT = ObjectType(
    "Query", fields[GnRepo, Unit](
        Field("nameString", ResponseOT,
          arguments = List(IdArg, PageArg, PerPageArg, WithSurrogatesArg, WithVernacularsArg),
          resolve = ctx => ctx.withArgs(IdArg, PageArg, PerPageArg, WithVernacularsArg) {
            (id, page, perPage, withVernaculars) =>
              ctx.ctx.nameStringByUuid(id, page, perPage, withVernaculars)
          }
        )
      , Field("nameResolvers", ListType(ResponseOT),
          arguments = List(NamesRequestArg, DataSourceIdsArg, PageArg, PerPageArg,
                           WithSurrogatesArg, WithVernacularsArg),
          resolve = ctx => ctx.withArgs(NamesRequestArg, DataSourceIdsArg, PageArg, PerPageArg,
                                        WithSurrogatesArg, WithVernacularsArg) {
            (names, dataSourceIdsOpt, page, perPage, withSurrogates, withVernaculars) =>
              val dataSourceIds = dataSourceIdsOpt.map { _.toVector }.getOrElse(Vector())
              ctx.ctx.nameStrings(names.take(nameStringsMaxCount), dataSourceIds, page, perPage,
                                  withSurrogates, withVernaculars)
          }
        )
      , Field("nameStrings", ResponseOT,
          arguments = List(SearchTermArg, PageArg, PerPageArg,
                           WithSurrogatesArg, WithVernacularsArg),
          resolve = ctx => ctx.withArgs(SearchTermArg, PageArg, PerPageArg, WithSurrogatesArg,
                                        WithVernacularsArg) { ctx.ctx.nameResolve })
      , Field("crossMap", ListType(CrossMap.ResultOT),
          arguments = List(CrossMap.DBSourceIdArg, CrossMap.DBTargetIdArg, CrossMap.SinksArg),
          resolve = ctx => ctx.withArgs(CrossMap.DBSourceIdArg, CrossMap.DBTargetIdArg,
                                        CrossMap.SinksArg) { ctx.ctx.crossMapResolve })
    )
  )

  val schema = Schema(QueryTypeOT)

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
