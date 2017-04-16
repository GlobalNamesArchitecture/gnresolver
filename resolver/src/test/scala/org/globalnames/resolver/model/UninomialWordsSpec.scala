package org.globalnames
package resolver
package model
package db

import slick.driver.PostgresDriver.api._
import java.util.UUID

class UninomialWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("UninomialWords") {
    val uninomialWords = TableQuery[UninomialWords]

    it("contains several records") {
      whenReady(conn.run(uninomialWords.size.result)) { res => res should be > 0 }
    }

    describe("#speciesWord") {
      it("returns an species word") {
        whenReady(conn.run(uninomialWords.map { _.uninomialWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(uninomialWords.map { _.nameStringUuid }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }
  }
}
