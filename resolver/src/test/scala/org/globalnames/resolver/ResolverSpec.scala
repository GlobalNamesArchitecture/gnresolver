package org.globalnames
package resolver

import java.util.UUID

import model.{NameStrings, NameString, Name}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite}
import org.scalatest.concurrent.ScalaFutures
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResolverSpec extends FunSuite with BeforeAndAfterEach
                   with BeforeAndAfterAll with ScalaFutures {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val names = Seq(
    NameString(Name(UUID.fromString("b0f8459f-8b73-514c-b6f3-568d54d99ded"),
                    "Salinator solida (Martens, 1878)"),
               Name(UUID.fromString("0e77a809-3c28-5bc7-9f0f-7045f4ae9f42"),
                    "SALINATOR SOLIDA (MARTENS, 1878)")),
    NameString(Name(UUID.fromString("f9638b29-b48f-5130-b469-ed0fa50ba5a8"),
                    "Tasmaphena sinclairi (Pfeiffer, 1846)"),
               Name(UUID.fromString("8cf8696e-6ca6-5ec7-b441-e04a37ea751c"),
                    "TASMAPHENA SINCLAIRI (PFEIFFER, 1846)"))
  )
  val canonicalNames = names.map { _.canonicalName }
  val matcher = Matcher(canonicalNames.map { _.value }, maxDistance = 2)

  override def beforeEach(): Unit = {
    val setup = DBIO.seq(
      nameStrings.schema.create,
      nameStrings ++= names
    )
    Await.result(conn.run(setup), 10.seconds)
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    try {
      super.afterEach()
    } finally {
      val cleanup = DBIO.seq(
        nameStrings.schema.drop
      )
      Await.result(conn.run(cleanup), 10.seconds)
    }
  }

  override def afterAll(): Unit = {
    conn.close()
  }

  test("must resolve by name UUID") {
    val resolver = new Resolver(conn, matcher)
    whenReady(resolver.resolve("Salinator solida (Martens, 1878)")) { r =>
      assert(r.byName.head.name.id ===
        UUID.fromString("b0f8459f-8b73-514c-b6f3-568d54d99ded"))
    }
  }

  test("must resolve by canonical name UUID") {
    val resolver = new Resolver(conn, matcher)
    whenReady(resolver.resolve("TASMAPHENA SINCLAIRI (PFEIFFER, 1846)")) { r =>
      assert(r.byCanonicalName.head.name.id ===
        UUID.fromString("f9638b29-b48f-5130-b469-ed0fa50ba5a8"))
    }
  }

  test("must resolve fuzzy by canonical name UUID") {
    val resolver = new Resolver(conn, matcher)
    whenReady(resolver.resolve("TASMAPHENA SINCLAIRI (PFEIFFxx, 1846)")) { r =>
      assert(r.byCanonicalNameFuzzy.head ===
        "TASMAPHENA SINCLAIRI (PFEIFFER, 1846)")
    }
  }
}
