package org.globalnames
package resolver
package api

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.typesafe.config.Config
import Resolver.NameRequest
import QueryParser.SearchPart
import model.Matches
import Materializer.Parameters
import slick.driver.PostgresDriver.api._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scalaz._
import Scalaz.{get => _, _}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import sangria.parser.{QueryParser => QueryParserSangria}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._

trait Service extends NamestringsProtocols with CrossMapProtocols with NullOptions {
  import SchemaDefinition.nameStringsMaxCount

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter
  val database: Database
  val matcher: Matcher
  lazy val resolver = new Resolver(database, matcher)
  lazy val facetedSearcher = new FacetedSearcher(database)
  lazy val searcher = new Searcher(database, resolver, facetedSearcher)
  lazy val crossMap = new CrossMapSearcher(database)

  def resolve(search: SearchPart, parameters: Parameters): Future[Matches] = {
    searcher.resolve(search.contents, search.modifier, search.wildcard, parameters)
  }

  val routes = {
    logRequestResult("gnresolver-microservice") {
      pathPrefix("api") {
        path("version") {
          complete {
            Map("version" -> BuildInfo.version)
          }
        } ~ (post & path("graphql")) {
          entity(as[JsValue]) { requestJson =>
            val JsObject(fields) = requestJson
            val JsString(query) = fields("query")
            val vars = fields.get("variables") match {
              case Some(obj: JsObject) => obj
              case _ => JsObject.empty
            }
            val operation = fields.get("operationName") collect {
              case JsString(op) => op
            }
            QueryParserSangria.parse(query) match {
              case util.Success(queryAst) =>
                complete(Executor.execute(SchemaDefinition.schema, queryAst,
                         SchemaDefinition.GnRepo(facetedSearcher, resolver, searcher, crossMap),
                         variables = vars, operationName = operation)
                  .map(OK -> _)
                  .recover {
                    case error: QueryAnalysisError => BadRequest -> error.resolveError
                    case error: ErrorWithResolver => InternalServerError -> error.resolveError
                  })
              case util.Failure(error) =>
                complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
            }
          }
        } ~ path("name_resolvers") {
          val getNameResolvers =
            get & parameters('names.as[Seq[NameRequest]], 'dataSourceIds.as[Vector[Int]].?,
                             'per_page ? nameStringsMaxCount, 'page ? 0,
                             'surrogates ? false, 'vernaculars ? false)
          val postNameResolvers =
            post & entity(as[Seq[NameRequest]]) &
              parameters('dataSourceIds.as[Vector[Int]].?,
                         'per_page ? nameStringsMaxCount, 'page ? 0, 'surrogates ? false,
                         'vernaculars ? false)

          (getNameResolvers | postNameResolvers) {
            (names, dataSourceIds, perPage, page, withSurrogates, withVernaculars) => complete {
              val params = Parameters(page, perPage, withSurrogates, withVernaculars)
              val matches = resolver.resolveExact(names.take(nameStringsMaxCount),
                                                  dataSourceIds.orZero, params)
              matches.map { ms => result(ms, page, perPage) }
            }
          }
        } ~ path("name_strings" / JavaUUID) { uuid =>
          (get & parameters('page ? 0, 'per_page ? 50, 'vernaculars ? false)) {
            (page, perPage, vernaculars) => complete {
              val params = Parameters(page, perPage, withSurrogates = false, vernaculars)
              facetedSearcher.findNameStringByUuid(uuid, params).map { m =>
                result(Seq(m), page, perPage)
              }
            }
          }
        } ~ path("name_strings" / Remaining) { remaining =>
          complete {
            result(Seq(Matches.empty(remaining)), 0, 0)
          }
        } ~ path("name_strings") {
          (get & parameters('search_term, 'per_page ? nameStringsMaxCount, 'page ? 0,
                            'surrogates ? false, 'vernaculars ? false)) {
            (searchTerm, perPage, page, withSurrogates, withVernaculars) => complete {
              val search = QueryParser.result(searchTerm)
              logger.debug(s"$search")
              val params = Parameters(page, perPage, withSurrogates, withVernaculars,
                                      query = searchTerm.some)
              resolve(search, params).map { m => result(Seq(m), page, perPage) }
            }
          }
        } ~ path("names_strings" / JavaUUID / "dataSources") { uuid =>
          get {
            complete {
              facetedSearcher.resolveDataSources(uuid)
            }
          }
        } ~ pathPrefix("crossmap") {
          (post & entity(as[CrossMapRequest]) &
            parameters('dbSourceId.as[Int], 'dbTargetId.as[Int].?)) {
            (crossMapReq, dbSourceId, dbTargetId) => complete {
              crossMap.execute(dbSourceId, crossMapReq.dbSinkIds, dbTargetId,
                crossMapReq.localIds)
            }
          }
        }
      }
    }
  }
}
