package org.globalnames
package resolver
package api

import java.io.File
import java.nio.file.Paths

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpEntity, StatusCode}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.commons.io.FileUtils
import model.Matches
import CrossMapSearcher.{Result, Source, Target}
import slick.driver.PostgresDriver.api._

import scala.collection.JavaConversions._
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValueType}
import spray.json._
import resolver.Ops._
import akka.http.scaladsl.server.Route

import scalaz._
import Scalaz._

class GnresolverMicroserviceIntegrationSpec extends SpecConfig with ApiSpecConfig
                                               with Service with ScalatestRouteTest {

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  override val database = Database.forConfig("postgresql-test")
  override val matcher = Matcher(Seq(), maxDistance = 2)

  describe("GnresolverMicroservice") {
    seed("test_api", "GnresolverMicroserviceIntegrationSpec")

    def test(description: String, requestConfig: Config,
             statusCode: StatusCode, responseBody: Option[JsValue]): Unit = {
      val method = requestConfig.getString("method")
      for (url <- requestConfig.getStringList("urls")) {
        val parameters = requestConfig.getOptionalObject("parameters").map { p =>
          p.entrySet().map { entry =>
            val valueStr = entry.getValue.valueType match {
              case ConfigValueType.STRING => entry.getValue.unwrapped().asInstanceOf[String]
              case _ => entry.getValue.render(ConfigRenderOptions.concise)
            }
            (entry.getKey, valueStr)
          }.toSeq
        }.getOrElse(Seq())
        val request = method match {
          case "GET" =>
            Get(Uri(url).withQuery(Query(parameters: _*)))
          case "POST" =>
            val data = requestConfig.getOptionalConfigValue("data").map { dataConf =>
              dataConf.render(ConfigRenderOptions.concise)
            }.orZero
            Post(Uri(url).withQuery(Uri.Query(parameters: _*)),
                 HttpEntity(`application/json`, data))
          case meth => fail(s"Method is not supported in testing: $meth")
        }

        it(s"${request.method} ${request.uri} ~> $statusCode") {
          request ~> Route.seal(routes) ~> check {
            status shouldBe statusCode
            if (responseBody.isDefined) {
              responseAs[String].parseJson shouldBe responseBody.value
            }
          }
        }
      }
    }

    val testFolder = "test_data"
    val testFolderUri = ClassLoader.getSystemResource(testFolder).toURI
    FileUtils.listFiles(new File(testFolderUri), Array("conf"), true).foreach { f =>
      val testFilePath = Paths.get(testFolder, testFolderUri.relativize(f.toURI).getPath)
                              .normalize.toString
      val conf = ConfigFactory.load(testFilePath)

      val description = conf.getString("description")
      val statusCode: StatusCode = conf.getInt("response.status")
      val responseBody = conf.getOptionalConfigValue("response.body").map { conf =>
                           conf.render(ConfigRenderOptions.concise).parseJson
                         }
      describe(s"$description ($testFilePath)") {
        for { request <- conf.getConfigList("requests") } {
          test(description, request, statusCode, responseBody)
        }
      }
    }

    describe("/name_resolvers") {
      describe("access by 'names' parameter") {
        describe("GET method") {
          it("filters results by 'local_id' setting") {
            val localId = 7
            val url = """/api/name_resolvers?""" +
              s"""names=[{"value":"Favorinus+horridus","localId":$localId}]"""

            Get(url) ~> routes ~> check {
              status shouldBe OK
              val response = responseAs[Seq[Matches]]
              response should have size 1

              response(0).total shouldBe 1
              response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"
              response(0).localId shouldBe Some(localId)
            }
          }

          it("filters results by 'dataSourceId' parameter") {
            Get("""/api/name_resolvers?dataSourceIds=[7]&""" +
              s"""names=[{"value":"Galeodila+somalica+Caporiacco+1945"}]""") ~>
              routes ~> check {
                status shouldBe OK
                val response = responseAs[Seq[Matches]]
                response should have size 1

                response(0).total shouldBe 1
                response(0).matches(0).nameString.name.value shouldBe
                  "Galeodila somalica Caporiacco 1945"
                response(0).matches(0).dataSource.id shouldBe 7
            }
          }
        }

        describe("POST method") {
          it("filters results by 'local_id' setting") {
            val localId = 7

            Post("/api/name_resolvers", HttpEntity(`application/json`,
              s"""[{"value":"Favorinus horridus", "localId": $localId}]""")) ~>
              routes ~> check {
              status shouldBe OK
              val response = responseAs[Seq[Matches]]
              response should have size 1

              response(0).total shouldBe 1
              response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"
              response(0).localId shouldBe Some(localId)
            }
          }

          it("filters results by 'dataSourceId' parameter") {
            Post("/api/name_resolvers?dataSourceIds=[7]", HttpEntity(`application/json`,
              """[{"value":"Galeodila somalica Caporiacco 1945"}]""")) ~> routes ~> check {
              status shouldBe OK
              val response = responseAs[Seq[Matches]]
              response should have size 1

              response(0).total shouldBe 1
              response(0).matches(0).nameString.name.value shouldBe
                "Galeodila somalica Caporiacco 1945"
              response(0).matches(0).dataSource.id shouldBe 7
            }
          }
        }
      }
    }

    describe("/crossmap") {
      describe("POST method") {
        it("maps one local id to another") {
          seed("test_api", "GnresolverMicroserviceIntegrationSpec_CrossMap")
          Post("/api/crossmap?dbSourceId=4&dbTargetId=11",
            HttpEntity(`application/json`, """{
                                              |"dbSinkIds":[179],
                                              |"localIds":["foo", "40800"]
                                              |}""".stripMargin)) ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Seq[Result]]
            response should have size 2
            val Seq(resp1, resp2) = response
            resp1.source shouldBe Source(4, "foo")
            resp2.source shouldBe Source(4, "40800")
            resp1.target shouldBe empty
            resp2.target should contain only Target(179, 11, "5371640")
          }
        }
      }
    }
  }
}
