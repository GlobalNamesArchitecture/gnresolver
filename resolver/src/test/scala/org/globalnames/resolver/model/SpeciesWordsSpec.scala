package org.globalnames
package resolver
package model
package db

import slick.driver.PostgresDriver.api._
import java.util.UUID

class SpeciesWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("SpeciesWords") {
    val speciesWords = TableQuery[SpeciesWords]

    it("contains several records") {
      whenReady(conn.run(speciesWords.size.result)) { res => res should be > -1 }
    }

    describe("#speciesWord") {
      it("returns an species word") {
        whenReady(conn.run(speciesWords.map { _.speciesWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(speciesWords.map { _.nameStringUuid }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }
  }
}
