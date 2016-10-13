package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._
import java.util.UUID

class SubspeciesWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("SubspeciesWords") {
    val subspeciesWords = TableQuery[SubspeciesWords]

    it("contains several records") {
      whenReady(conn.run(subspeciesWords.size.result)) { res => res should be > -1 }
    }

    describe("#subspeciesWord") {
      it("returns an subspecies word") {
        whenReady(conn.run(subspeciesWords.map { _.subspeciesWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(subspeciesWords.map { _.nameStringUuid }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }
  }
}

