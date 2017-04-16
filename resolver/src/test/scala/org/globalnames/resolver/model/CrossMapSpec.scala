package org.globalnames
package resolver
package model
package db

import slick.driver.PostgresDriver.api._
import java.util.UUID

class CrossMapSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "CrossMapIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("CrossMap") {
    val crossMaps = TableQuery[CrossMaps]

    it("contains several records") {
      whenReady(conn.run(crossMaps.size.result)) { res => res should be > 1 }
    }

    describe("#dataSourceId") {
      it("returns a dataSourceId") {
        whenReady(conn.run(crossMaps.map { _.dataSourceId }.result.head)) { res =>
          res shouldBe a[Integer]
        }
      }
    }

    describe("#nameStringId") {
      it("returns a name_string UUID") {
        whenReady(conn.run(crossMaps.map { _.nameStringId }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }

    describe("#localId") {
      it("returns a localId") {
        whenReady(conn.run(crossMaps.map { _.localId }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }


    describe("#dataSourceIdCrossMap") {
      it("returns a dataSourceId for cross_map") {
        whenReady(conn.run(crossMaps.map { _.dataSourceIdCrossMap }.result.head)) { res =>
          res shouldBe an[Integer]
        }
      }
    }


    describe("#taxonId") {
      it("returns a taxon id") {
        whenReady(conn.run(crossMaps.map { _.taxonId }.result.head)) { res =>
          res shouldBe an[String]
        }
      }
    }
  }
}
