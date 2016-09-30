package org.globalnames
package resolver

import java.util.UUID

import org.globalnames.resolver.model.NameStrings
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResolverIntegrationSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  var matcher: Matcher = _

  seed("test_resolver", "ResolverIntegrationSpec")

  override def beforeAll(): Unit = {
    super.beforeAll()

    val canonicalNamesQuery =
      nameStrings.filter { _.canonical.isDefined }.map { _.canonical.get }
    val canonicalNames = Await.result(conn.run(canonicalNamesQuery.result), 5.seconds)
    matcher = Matcher(canonicalNames, maxDistance = 2)
  }

  override def afterAll(): Unit = {
    conn.close()
  }

  "Resolver" should {
    "support general resolve" when {
      "exact match by name UUID" in {
        val resolver = new Resolver(conn, matcher)
        whenReady(resolver.resolveStrings(
          Seq("Pteroplatus arrogans BUQUET Jean Baptiste Lucien, 1840"))) { res =>
            res.size shouldBe 1

          res.head.matches.head.nameString.name.id shouldBe
            UUID.fromString("405b2394-d89f-52df-a5bd-efd195f3b33f")
        }
      }

      "exact match by canonical name UUID" in {
        val resolver = new Resolver(conn, matcher)
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogans"))) { res =>
          res.size shouldBe 1

          res.head.matches.head.nameString.canonicalName.value.id shouldBe
            UUID.fromString("9669d573-ff19-59fa-87c3-258a9058d6d2")
        }
      }

      "fuzzy match by canonical name UUID" in {
        val resolver = new Resolver(conn, matcher)
        whenReady(resolver.resolveStrings(Seq("Pteroplatus arrogaxx"))) { res =>
          res.size shouldBe 1

          res.head.matches.head.nameString.canonicalName.value.value shouldBe "Pteroplatus arrogans"
        }
      }
    }
  }
}
