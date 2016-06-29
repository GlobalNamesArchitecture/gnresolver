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

class GnresolverMicroserviceIntegrationSpec extends SpecConfig with Service
                                               with ScalatestRouteTest {
  override val config   = ConfigFactory.load()
  override val logger   = Logging(system, getClass)
  override val database = Database.forConfig("postgresql-test")
  override val matcher  = Matcher(Seq(), maxDistance = 2)

  "GnresolverMicroservice" should {
    "handle version request" in {
      Get("/api/version") ~> routes ~> check {
        status shouldBe OK
        responseAs[String] shouldBe BuildInfo.version
      }
    }

    "handle `names` request" when {
      seed("test_api", "GnresolverMicroserviceIntegrationSpec")

      "'GET'" in {
        Get("/api/names?v=Favorinus+horridus|Stegia+lavatera") ~> routes ~> check {
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
        Post("/api/names", HttpEntity(`application/json`,
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
}
