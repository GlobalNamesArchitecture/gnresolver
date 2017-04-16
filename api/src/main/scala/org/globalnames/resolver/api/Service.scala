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
import slick.jdbc.PostgresProfile.api._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scalaz._
import Scalaz.{get => _, _}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import sangria.parser.{QueryParser => QueryParserSangria}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._

trait Service extends SchemaDefinition with NullOptions {
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
                complete(Executor.execute(schema, queryAst,
                         GnRepo(facetedSearcher, resolver, searcher, crossMap),
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
        }
      }
    }
  }
}
