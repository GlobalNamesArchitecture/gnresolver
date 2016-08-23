package org.globalnames
package resolver
package api

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import org.globalnames.resolver.Resolver.Matches
import slick.driver.PostgresDriver.api._

class GnresolverMicroserviceIntegrationSpec extends SpecConfig with ApiSpecConfig
                                               with Service with ScalatestRouteTest {

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  override val database = Database.forConfig("postgresql-test")
  override val matcher = Matcher(Seq(), maxDistance = 2)

  "GnresolverMicroservice" should {
    "handle version request" in {
      Get("/api/version") ~> routes ~> check {
        status shouldBe OK
        responseAs[String] shouldBe BuildInfo.version
      }
    }

    "handle requests" when {
      seed("test_api", "GnresolverMicroserviceIntegrationSpec")

      "handle `name_strings`/uuid request" when {
        "existing name" in {
          Get("/api/name_strings/b701ec9e-efb0-5d5b-bf03-b920c00d0a77") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Matches]
            response.matches.size shouldBe 1
            response.matches(0).nameString.name.value shouldBe "Favorinus horridus"
          }
        }

        "non-existing name" in {
          Get("/api/name_strings/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Matches]
            response.matches shouldBe empty
          }
        }
      }

      "handle `name_resolvers` requests" when {
        "'GET'" in {
          Get("/api/name_resolvers?names=Favorinus+horridus|Stegia+lavatera") ~> routes ~> check {
            status shouldBe OK
            val response = responseAs[Seq[Matches]]
            response.size shouldBe 2

            response(0).total shouldBe 1
            response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"

            response(1).total shouldBe 1
            response(1).matches(0).nameString.name.value shouldBe "Stegia lavatera"
          }
        }

        "'POST'" in {
          Post("/api/name_resolvers", HttpEntity(`application/json`,
            """["Favorinus horridus", "Stegia lavatera"]""")) ~>
            routes ~> check {
            status shouldBe OK
            val response = responseAs[Seq[Matches]]
            response.size shouldBe 2

            response(0).total shouldBe 1
            response(0).matches(0).nameString.name.value shouldBe "Favorinus horridus"

            response(1).total shouldBe 1
            response(1).matches(0).nameString.name.value shouldBe "Stegia lavatera"
          }
        }
      }
    }

    "handle `cross_map` request" in {
      seed("test_api", "GnresolverMicroserviceIntegrationSpec_CrossMap")

      Post("/api/crossmap?dbSourceId=8&dbTargetId=178",
        HttpEntity(`application/json`, """["1", "1005101"]""")) ~> routes ~> check {
        status shouldBe OK
        val response = responseAs[Seq[(String, String)]]
        response.size shouldBe 2

        response should contain (("1", ""))
        response should contain (("1005101", "AF468436/#6"))
      }
    }
  }
}