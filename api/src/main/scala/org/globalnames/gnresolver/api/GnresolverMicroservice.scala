package org.globalnames
package gnresolver
package api

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.globalnames.gnresolver.api.QueryParser.{Modifier, SearchPart}
import org.globalnames.resolver.Resolver
import org.globalnames.resolver.Resolver.{Match, Kind}
import org.globalnames.resolver.model.{Name, NameString}
import slick.driver.PostgresDriver.api._
import spray.json._
import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.Source

case class Request(names: Seq[String])
case class QueryNames(value: String)

trait Protocols extends DefaultJsonProtocol {
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString)
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x =>
        deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
  implicit object KindJsonFormat extends RootJsonFormat[Kind] {
    def write(x: Kind) = JsString(x.toString)
    def read(value: JsValue) = value match {
      case JsString("") => ???
      case x =>
        deserializationError("Expected Kind as JsString, but got " + x)
    }
  }
  implicit val nameFormat       = jsonFormat2(Name.apply)
  implicit val nameStringFormat = jsonFormat2(NameString.apply)
  implicit val matchFormat      = jsonFormat2(Match.apply)
  implicit val requestFormat    = jsonFormat1(Request.apply)
  implicit val queryNamesFormat = jsonFormat1(QueryNames.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter
  val database: Database
  val matcher: Matcher
  lazy val resolver = new Resolver(database, matcher)

  def resolve(search: SearchPart, take: Int, drop: Int): Future[Seq[Match]] = {
    search.modifier match {
      case Modifier(QueryParser.noModifier) =>
        resolver.resolve(search.contents, take, drop)
      case Modifier(QueryParser.canonicalModifier) =>
        if (search.wildcard) {
          resolver.resolveCanonicalLike(search.contents + '%', take, drop)
        } else {
          resolver.resolveCanonical(search.contents, take, drop)
        }
      case Modifier(QueryParser.authorModifier) =>
        resolver.resolveAuthor(search.contents, take, drop)
      case Modifier(QueryParser.yearModifier) =>
        resolver.resolveYear(search.contents, take, drop)
      case Modifier(QueryParser.uninomialModifier) =>
        resolver.resolveUninomial(search.contents, take, drop)
      case Modifier(QueryParser.genusModifier) =>
        resolver.resolveGenus(search.contents, take, drop)
      case Modifier(QueryParser.speciesModifier) =>
        resolver.resolveSpecies(search.contents, take, drop)
      case Modifier(QueryParser.subspeciesModifier) =>
        resolver.resolveSubspecies(search.contents, take, drop)
      case Modifier(QueryParser.nameStringModifier) => ???
      case Modifier(QueryParser.exactModifier) => ???
    }
  }

  val routes = {
    logRequestResult("gnresolver-microservice") {
      pathPrefix("api") {
        path("version") {
          complete {
            "0.1.0-SNAPSHOT"
          }
        } ~ path("names") {
          get {
            parameter('v) { values =>
              complete {
                val res =
                  values.split("\\|")
                    .take(1000)
                    .map { n => resolver.resolve(n) }
                    .toSeq
                Future.sequence(res)
              }
            }
          } ~ (post & entity(as[Request])) { request =>
            complete {
              val res =
                request.names
                  .take(1000)
                  .map { n => resolver.resolve(n) }
              Future.sequence(res)
            }
          }
        } ~ path("search") {
          get {
            parameters('v, 'take ? 1000, 'drop ? 0) {
              (value, take, drop) =>
                complete {
                  val search = QueryParser.result(value)
                  logger.debug(s"$search")
                  resolve(search, take, drop)
                }
            }
          }
        }
      }
    }
  }
}

object GnresolverMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config   = ConfigFactory.load()
  override val logger   = Logging(system, getClass)
  override val database = Database.forConfig("postgresql")
  override val matcher  =
    Matcher(Source.fromFile("./name_strings_canonical.tsv").getLines.toList,
            maxDistance = 2)

  Http().bindAndHandle(routes,
                       config.getString("http.interface"),
                       config.getInt("http.port"))
}
