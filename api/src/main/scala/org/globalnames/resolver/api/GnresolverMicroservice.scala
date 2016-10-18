package org.globalnames
package resolver
package api

import java.io.File
import java.nio.file.Paths
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import Resolver.NameRequest
import QueryParser.SearchPart
import model.{DataSource, Kind, Match, Matches, Name, NameString, NameStringIndex, NameStrings}
import slick.driver.PostgresDriver.api._
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scalaz._
import Scalaz.{get => _, _}

trait Protocols extends DefaultJsonProtocol {
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID): JsString = JsString(x.toString)
    def read(value: JsValue): UUID = value match {
      case JsString(x) => UUID.fromString(x)
      case x =>
        deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
  implicit object KindJsonFormat extends RootJsonFormat[Kind] {
    def write(x: Kind): JsString = JsString(x.toString)
    def read(value: JsValue): Kind = value match {
      case JsString("None") => Kind.None
      case JsString("Fuzzy") => Kind.Fuzzy
      case JsString("ExactNameMatchByUUID") => Kind.ExactNameMatchByUUID
      case JsString("ExactCanonicalNameMatchByUUID") => Kind.ExactCanonicalNameMatchByUUID
      case x => deserializationError("Expected Kind as JsString, but got " + x)
    }
  }
  implicit val nameFormat = jsonFormat2(Name.apply)
  implicit val nameStringFormat = jsonFormat2(NameString.apply)
  implicit val nameStringIndexFormat = jsonFormat3(NameStringIndex.apply)
  implicit val dataSourceFormat = jsonFormat3(DataSource.apply)
  implicit val matchFormat = jsonFormat3(Match.apply)
  implicit val matchesFormat = jsonFormat4(Matches.apply)
  implicit val nameRequestFormat = jsonFormat2(NameRequest.apply)

  implicit def unmarshalStringJson2NameRequest: Unmarshaller[String, Seq[NameRequest]] =
    Unmarshaller.strict((_: String).parseJson match {
      case JsArray(objs: Vector[JsValue]) =>
        objs.map { case obj: JsObject => nameRequestFormat.read(obj) }
      case obj: JsObject => Seq(nameRequestFormat.read(obj))
    })

  implicit def unmarshalStringJson2SeqInt: Unmarshaller[String, Vector[Int]] =
    Unmarshaller.strict((_: String).parseJson match {
      case JsArray(xs: Vector[JsValue]) => xs.map { case x: JsNumber => x.value.toInt }
    })
}

trait Service extends Protocols {
  private val nameStringsMaxCount = 1000

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter
  val database: Database
  val matcher: Matcher
  lazy val resolver = new Resolver(database, matcher)
  lazy val facetedSearcher = new FacetedSearcher(database)
  lazy val searcher: Searcher = new Searcher(resolver, facetedSearcher)
  lazy val crossMap = new CrossMap(database)

  def resolve(search: SearchPart, take: Int, drop: Int): Future[Matches] = {
    searcher.resolve(search.contents, search.modifier, search.wildcard, take, drop)
  }

  val routes = {
    logRequestResult("gnresolver-microservice") {
      pathPrefix("api") {
        path("version") {
          complete {
            Map("version" -> BuildInfo.version)
          }
        } ~ path("name_resolvers") {
          val getNameResolvers =
            get & parameters('names.as[Seq[NameRequest]], 'dataSourceIds.as[Vector[Int]] ?)
          val postNameResolvers =
            post & entity(as[Seq[NameRequest]]) & parameter('dataSourceIds.as[Vector[Int]] ?)

          (getNameResolvers | postNameResolvers) {
            (names, dataSourceIds) => complete {
              resolver.resolve(names.take(nameStringsMaxCount), dataSourceIds.orZero)
            }
          }
        } ~ path("name_strings" / JavaUUID) { uuid =>
          complete {
            facetedSearcher.findNameStringByUuid(uuid)
          }
        } ~ path("name_strings" / Remaining) { remaining =>
          complete {
            Matches.empty(remaining)
          }
        } ~ path("name_strings") {
          (get & parameters('search_term, 'take ? nameStringsMaxCount, 'drop ? 0)) {
            (searchTerm, take, drop) => complete {
              val search = QueryParser.result(searchTerm)
              logger.debug(s"$search")
              resolve(search, take, drop)
            }
          }
        } ~ path("names" / JavaUUID / "dataSources") { uuid =>
          get {
            complete {
              facetedSearcher.resolveDataSources(uuid)
            }
          }
        } ~ pathPrefix("crossmap") {
          (post & entity(as[Seq[String]]) &
            parameters('dbSourceId.as[Int], 'dbTargetId.as[Int])) {
              (localIds, dbSourceId, dbTargetId) => complete {
                crossMap.execute(dbSourceId, dbTargetId, localIds)
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

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  override val database = Database.forConfig("postgresql")
  override val matcher = {
    logger.info("Matcher: restoring")
    val dumpPath = {
      val dumpFolder = {
        val folder = config.getString("gnresolver.gnmatcher-dump-folder")
        folder.isEmpty ? System.getProperty("java.io.tmpdir") | folder
      }
      val dumpFile = config.getString("gnresolver.gnmatcher-dump-file")
      Paths.get(dumpFolder, dumpFile).toString
    }
    val useDump = config.getBoolean("gnresolver.gnmatcher-use-dump")
    logger.info(s"Matcher: using dump file '$dumpPath' -- $useDump")
    def createMatcher = {
      val nameStrings = scala.concurrent.Await.result(
        database.run(TableQuery[NameStrings].map { _.canonical }.result.map { _.flatten }),
        5.seconds
      )
      Matcher(nameStrings, maxDistance = 2)
    }
    val matcher = (useDump, new File(dumpPath).exists()) match {
      case (true, true) => Matcher.restore(dumpPath)
      case (true, false) =>
        val matcher = createMatcher
        matcher.dump(dumpPath)
        matcher
      case (false, _) => createMatcher
    }
    logger.info("Matcher: restored")
    matcher
  }

  Http().bindAndHandle(routes,
                       config.getString("http.interface"),
                       config.getInt("http.port"))
}
