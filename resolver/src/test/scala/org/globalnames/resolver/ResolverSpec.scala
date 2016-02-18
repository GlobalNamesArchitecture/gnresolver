package org.globalnames.resolver

import java.util.UUID

import org.globalnames.resolver.model.NameStrings
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.concurrent.ScalaFutures
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class ResolverSpec extends FunSuite with BeforeAndAfterEach with ScalaFutures {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]

  override def beforeEach(): Unit = {
    val setup = DBIO.seq(
      nameStrings.schema.create,
      nameStrings ++= Seq(
        (UUID.fromString("b0f8459f-8b73-514c-b6f3-568d54d99ded"),
          "Salinator solida (Martens, 1878)",
          UUID.fromString("0e77a809-3c28-5bc7-9f0f-7045f4ae9f42"),
          "SALINATOR SOLIDA (MARTENS, 1878)"),
        (UUID.fromString("f9638b29-b48f-5130-b469-ed0fa50ba5a8"),
          "Tasmaphena sinclairi (Pfeiffer, 1846)",
          UUID.fromString("8cf8696e-6ca6-5ec7-b441-e04a37ea751c"),
          "TASMAPHENA SINCLAIRI (PFEIFFER, 1846)")
      )
    )
    Await.result(conn.run(setup), 10.seconds)
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    try {
      super.afterEach()
      val cleanup = DBIO.seq(
        nameStrings.schema.drop
      )
      Await.result(conn.run(cleanup), 10.seconds)
    } finally {
      conn.close()
    }
  }

  test("must resolve by name UUID") {
    val resolver = new Resolver(conn)
    whenReady(resolver.resolve("Salinator solida (Martens, 1878)")) { result =>
      assert(result.head._1 === UUID.fromString("b0f8459f-8b73-514c-b6f3-568d54d99ded"))
    }
    whenReady(resolver.resolve("TASMAPHENA SINCLAIRI (PFEIFFER, 1846)")) { result =>
      assert(result.head._1 === UUID.fromString("f9638b29-b48f-5130-b469-ed0fa50ba5a8"))
    }
  }
}
