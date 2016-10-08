package org.globalnames
package resolver

import java.util.UUID

import model.NameStrings
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResolverIntegrationSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  var matcher: Matcher = _
  var resolver: Resolver = _

  seed("test_resolver", "ResolverIntegrationSpec")

  override def beforeAll(): Unit = {
    super.beforeAll()

    val canonicalNamesQuery =
      nameStrings.filter { _.canonical.isDefined }.map { _.canonical.get }
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
                  Seq("Pteroplatus arrogans BUQUET Jean Baptiste Lucien, 1840"))) { res =>
          res.size shouldBe 1
          res.head.matches.head.nameString.name.id shouldBe
            UUID.fromString("405b2394-d89f-52df-a5bd-efd195f3b33f")
        }
      }

      it("exact matches by canonical form") {
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogans"))) { res =>
          res.size shouldBe 1
          res.head.matches.head.nameString.canonicalName.value.id shouldBe
            UUID.fromString("9669d573-ff19-59fa-87c3-258a9058d6d2")
        }
      }

      it("fuzzy matches by canonical form") {
        pending
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogaxx"))) { res =>
          res.size shouldBe 1
          res.head.matches.head.nameString.canonicalName.value.value shouldBe "Pteroplatus arrogans"
        }
      }

    }
  }
}
