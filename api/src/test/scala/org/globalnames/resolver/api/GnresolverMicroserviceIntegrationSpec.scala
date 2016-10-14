package org.globalnames
package resolver
package api

import java.io.File
import java.nio.file.Paths

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpEntity, StatusCode}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.commons.io.FileUtils
import org.globalnames.resolver.model.Matches
import slick.driver.PostgresDriver.api._

import scala.collection.JavaConversions._
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import spray.json._
import DefaultJsonProtocol._
import Ops._
import akka.http.scaladsl.server.Route

class GnresolverMicroserviceIntegrationSpec extends SpecConfig with ApiSpecConfig
                                               with Service with ScalatestRouteTest {

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  override val database = Database.forConfig("postgresql-test")
  override val matcher = Matcher(Seq(), maxDistance = 2)

  describe("GnresolverMicroservice") {
    seed("test_api", "GnresolverMicroserviceIntegrationSpec")

    def test(description: String, method: String, url: String,
             statusCode: StatusCode, responseBody: Option[JsValue]): Unit = {
        it(s"$method $url ~> $statusCode") {
        val request = method match {
          case "GET" => Get(url)
          case "POST" => Post(url)
          case meth => fail(s"Method is not supported in testing: $meth")
        }

        request ~> Route.seal(routes) ~> check {
          status shouldBe statusCode
          if (responseBody.isDefined) {
            responseAs[String].parseJson shouldBe responseBody.value
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
      val responseBody = conf.getOptionalObject("response.body").map { obj =>
                           obj.render(ConfigRenderOptions.concise).parseJson
                         }
      describe(description) {
        for { url <- conf.getStringList("request.urls")
              method <- conf.getStringList("request.methods") } {
          test(description, method, url, statusCode, responseBody)
        }
      }
    }

    describe("/name_strings") {
      describe("access by UUID") {
        it("returns one record if found") {
          Get("/api/name_strings/b701ec9e-efb0-5d5b-bf03-b920c00d0a77") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Matches]
            response.matches should have size 1
            response.matches(0).nameString.name.value shouldBe "Favorinus horridus"
          }
        }

        it("works with capitalized UUIDs") {
          Get("/api/name_strings/B701EC9E-EFB0-5D5B-BF03-B920C00D0A77") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Matches]
            response.matches should have size 1
            response.matches(0).nameString.name.value shouldBe "Favorinus horridus"
          }
        }

        it("returns no records with unknown UUID") {
          Get("/api/name_strings/aaaaaaaa-efb0-5d5b-bf03-b920c00d0a77") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Matches]
            response.matches shouldBe empty
          }
        }

        it("returns no matches with bad UUIDS") {
          val uuids = Seq("aaaaaaaa-efb0", "aaaaaa-efb0/abc/uh", "{}[]ac")
          for (uuid <- uuids) {
            Get("/api/name_strings/" + uuid) ~> routes ~> check {
              status shouldBe OK
              responseAs[Matches] shouldBe Matches.empty(Path(uuid).toString)
            }
          }
        }
      }
    }

    describe("/name_resolvers") {
      describe("access by 'names' parameter") {
        describe("GET method") {
          it("returns records for known names") {
            Get("""/api/name_resolvers?""" +
                """names=[{"value":"Favorinus+horridus"}]""") ~>
            routes ~> check {
              status shouldBe OK
              val response = responseAs[Seq[Matches]]
              response should have size 1

              response(0).total shouldBe 1
              response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"
              response(0).localId shouldBe None
            }
          }

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
                response(0).matches(0).dataSourceId shouldBe 7
            }
          }
        }

        describe("POST method") {
          it("returns records for known names") {
            Post("/api/name_resolvers", HttpEntity(`application/json`,
              """[{"value":"Favorinus horridus"}]""")) ~>
              routes ~> check {
              status shouldBe OK
              val response = responseAs[Seq[Matches]]
              response should have size 1

              response(0).total shouldBe 1
              response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"
              response(0).localId shouldBe None
            }
          }


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
              response(0).matches(0).dataSourceId shouldBe 7
            }
          }
        }
      }
    }

    describe("/crossmap") {
      describe("POST method") {
        it("maps one local id to another") {
          seed("test_api", "GnresolverMicroserviceIntegrationSpec_CrossMap")
          Post("/api/crossmap?dbSourceId=8&dbTargetId=178",
            HttpEntity(`application/json`, """["1", "1005101"]""")) ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Seq[(String, String)]]
            response should have size 2

            response should contain (("1", ""))
            response should contain (("1005101", "AF468436/#6"))
          }
        }
      }
    }
  }
}
