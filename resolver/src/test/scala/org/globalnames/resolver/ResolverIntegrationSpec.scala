package org.globalnames
package resolver

import java.util.UUID

import model.{MatchType, NameStrings}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

import Materializer.Parameters

class ResolverIntegrationSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  var matcher: Matcher = _
  var resolver: Resolver = _
  val parameters = Parameters(page = 0, perPage = 50,
                              withSurrogates = false, withVernaculars = false)

  seed("test_resolver", "ResolverIntegrationSpec")

  override def beforeAll(): Unit = {
    super.beforeAll()

    val canonicalNamesQuery = nameStrings.filter { _.canonical.isDefined }.map { _.canonical.get }
    val canonicalNames = Await.result(conn.run(canonicalNamesQuery.result), 5.seconds)
    matcher = Matcher(canonicalNames, maxDistance = 2)
    resolver = new Resolver(conn, matcher)
  }

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("Resolver") {

    describe(".resolve") {
      it("exact matches by name-string") {
        whenReady(resolver.resolveStrings(
                Seq("Pteroplatus arrogans BUQUET Jean Baptiste Lucien, 1840"), parameters)) { res =>
          res should have size 1
          res.head.matches.head.nameString.name.id shouldBe
            UUID.fromString("405b2394-d89f-52df-a5bd-efd195f3b33f")
          res.head.matches.head.matchType shouldBe MatchType.ExactNameMatchByUUID
        }
      }

      it("exact matches by canonical form") {
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogans"), parameters)) { res =>
          res should have size 1
          res.head.matches.head.nameString.canonicalName.value.id shouldBe
            UUID.fromString("9669d573-ff19-59fa-87c3-258a9058d6d2")
          res.head.matches.head.matchType shouldBe MatchType.ExactCanonicalNameMatchByUUID
        }
      }

      it("fuzzy matches by canonical form") {
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogaxx"), parameters)) { res =>
          res should have size 1
          res.head.matches.head.nameString.canonicalName.value.value shouldBe "Pteroplatus arrogans"
          res.head.matches.head.matchType shouldBe MatchType.Fuzzy
        }
      }

      it("handles capitalization of request") {
        whenReady(resolver.resolveStrings(Seq("pteroplatus arrogaxx"), parameters)) { res =>
          res should have size 1
          res.head.suppliedNameString shouldBe "Pteroplatus arrogaxx"
        }
      }
    }
  }
}
