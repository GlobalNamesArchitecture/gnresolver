package org.globalnames
package resolver

import java.util.UUID

import model.NameStrings
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResolverSpec extends WordSpec with Matchers with OptionValues with BeforeAndAfterEach
                   with BeforeAndAfterAll with ScalaFutures with SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  var matcher: Matcher = _

  override def beforeAll(): Unit = {
    val canonicalNamesQuery =
      nameStrings.filter { _.canonical.isDefined }.map { _.canonical.get }
    val canonicalNames = Await.result(conn.run(canonicalNamesQuery.result), 5.seconds)
    matcher = Matcher(canonicalNames, maxDistance = 2)
  }

  override def afterAll(): Unit = {
    conn.close()
  }

  "Resolver" should {
    "resolve exact match by name UUID" in {
      val resolver = new Resolver(conn, matcher)
      whenReady(resolver.resolve("Stelletta cyathoides Burton 1926")) { r =>
        r.matches.size shouldBe 1

        r.matches.head.nameString.name.id shouldBe
          UUID.fromString("5477686c-260f-5762-82f5-1737d850f943")
      }
    }

    "resolve exact match by canonical name UUID" in {
      val resolver = new Resolver(conn, matcher)
      whenReady(resolver.resolve("Pteroplatus arrogans")) { r =>
        r.matches.size shouldBe 1

        r.matches.head.nameString.canonicalName.value.id shouldBe
          UUID.fromString("9669d573-ff19-59fa-87c3-258a9058d6d2")
      }
    }

    "resolve fuzzy by canonical name UUID" in {
      val resolver = new Resolver(conn, matcher)
      whenReady(resolver.resolve("Storeus scutellarxx")) { r =>
        r.matches.size shouldBe 1

        r.matches.head.nameString.canonicalName.value.value shouldBe "Storeus scutellaris"
      }
    }
  }
}
