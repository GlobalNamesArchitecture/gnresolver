package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._
import java.util.UUID

class YearWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("YearWords") {
    val yearWords = TableQuery[YearWords]

    it("contains several records") {
      whenReady(conn.run(yearWords.size.result)) { res => res should be > -1 }
    }

    describe("#yearWord") {
      it("returns an year word") {
        whenReady(conn.run(yearWords.map { _.yearWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(yearWords.map { _.nameStringUuid }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }
  }
}
