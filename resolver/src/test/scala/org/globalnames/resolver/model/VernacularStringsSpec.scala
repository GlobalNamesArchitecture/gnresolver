package org.globalnames
package resolver
package model

import java.util.UUID

import slick.driver.PostgresDriver.api._

class VernacularStringsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "VernacularStringsSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("VernacularStrings") {
    val vernacularStrings = TableQuery[VernacularStrings]

    it("contains several records") {
      whenReady(conn.run(vernacularStrings.size.result)) { res => res should be > 1 }
    }

    describe("#id") {
      it("returns an id") {
        whenReady(conn.run(vernacularStrings.map { _.id }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }

    describe("#name") {
      it("returns a name") {
        whenReady(conn.run(vernacularStrings.map { _.name }.result.head)) { res =>
          res shouldBe an[String]
        }
      }
    }
  }
}
