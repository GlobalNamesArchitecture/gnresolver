package org.globalnames
package gnresolver
package api

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import org.globalnames.resolver.Resolver
import org.globalnames.resolver.Resolver.Match
import org.globalnames.resolver.model.{Name, NameString}
import slick.driver.PostgresDriver.api._
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class Request(names: Seq[String])
case class Response(matches: Seq[Match])

trait Protocols extends DefaultJsonProtocol {
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString)
    def read(value: JsValue) = value match {
        case JsString(x) => UUID.fromString(x)
        case x           =>
          deserializationError("Expected UUID as JsString, but got " + x)
      }
  }
  implicit val nameFormat       = jsonFormat2(Name.apply)
  implicit val nameStringFormat = jsonFormat2(NameString.apply)
  implicit val matchFormat      = jsonFormat3(Match.apply)
  implicit val requestFormat    = jsonFormat1(Request.apply)
  implicit val responseFormat   = jsonFormat1(Response.apply)
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

  val routes = {
    logRequestResult("gnresolver-microservice") {
      pathPrefix("api") {
        path("names") {
          get {
            parameter('v) { values =>
              complete {
                val res =
                  values.split("\\|")
                    .take(1000)
                    .map { n => resolver.resolve(n) }
                    .toSeq
                Future.sequence(res).map { (x: Seq[Match]) => Response(x) }
              }
            }
          } ~ (post & entity(as[Request])) { request =>
            complete {
              val res =
                request.names
                  .take(1000)
                  .map { n => resolver.resolve(n) }
              Future.sequence(res).map { (x: Seq[Match]) => Response(x) }
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
  override val matcher  = Matcher(Seq("Homo sapiense", "Urfus"), maxDistance = 2)

  Http().bindAndHandle(routes,
                       config.getString("http.interface"),
                       config.getInt("http.port"))
}
