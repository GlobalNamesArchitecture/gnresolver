package org.globalnames
package resolver
package model
package db

import slick.driver.PostgresDriver.api._
import java.util.UUID

class GenusWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("GenusWords") {
    val genusWords = TableQuery[GenusWords]

    it("contains several records") {
      whenReady(conn.run(genusWords.size.result)) { res => res should be > 1 }
    }

    describe("#speciesWord") {
      it("returns an species word") {
        whenReady(conn.run(genusWords.map { _.genusWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(genusWords.map { _.nameStringUuid }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }
  }
}
