package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._
import java.util.UUID

class AuthorWordsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("AuthorWords") {
    val authorWords = TableQuery[AuthorWords]

    it("contains several records") {
      whenReady(conn.run(authorWords.size.result)) { res => res should be > 1 }
    }

    describe("#authorWord") {
      it("returns an author word") {
        whenReady(conn.run(authorWords.map { _.authorWord }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#nameStringUuid") {
      it("returns an name_string UUID") {
        whenReady(conn.run(authorWords
          .map { _.nameStringUuid }.result.head)) { res =>
            res shouldBe an[UUID]
        }
      }
    }
  }
}
