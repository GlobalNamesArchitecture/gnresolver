package org.globalnames
package resolver
package model
package db

import java.util.UUID

import slick.driver.PostgresDriver.api._

class VernacularStringIndicesSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "VernacularStringsSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("VernacularStringIndices") {
    val vernacularStringIndices = TableQuery[VernacularStringIndices]

    it("contains several records") {
      whenReady(conn.run(vernacularStringIndices.size.result)) { res => res should be > 1 }
    }

    describe("#dataSourceId") {
      it("returns a dataSourceId") {
        whenReady(conn.run(vernacularStringIndices.map { _.dataSourceId }.result.head)) { res =>
          res shouldBe an[Integer]
        }
      }
    }

    describe("#taxonId") {
      it("returns a taxonId") {
        whenReady(conn.run(vernacularStringIndices.map { _.taxonId }.result.head)) { res =>
          res shouldBe an[String]
        }
      }
    }

    describe("#vernacularStringId") {
      it("returns a vernacularStringId") {
        whenReady(conn.run(vernacularStringIndices.map { _.vernacularStringId }.result.head)) {
          res => res shouldBe an[UUID]
        }
      }
    }

    describe("#language") {
      it("returns a language") {
        whenReady(conn.run(vernacularStringIndices.map { _.language }.result.head)) { res =>
          res.value shouldBe a[String]
        }
      }
    }

    describe("#locality") {
      it("returns a locality") {
        whenReady(conn.run(vernacularStringIndices.map { _.locality }.result.head)) { res =>
          res.value shouldBe a[String]
        }
      }
    }

    describe("#countryCode") {
      it("returns a countryCode") {
        whenReady(conn.run(vernacularStringIndices.map { _.countryCode }.result.head)) { res =>
          res.value shouldBe a[String]
        }
      }
    }
  }
}
