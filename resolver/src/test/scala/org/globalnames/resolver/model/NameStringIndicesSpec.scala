package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._

import java.util.UUID

class NameStringIndicesSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("NameStringIndices") {
    val nameStringIndices = TableQuery[NameStringIndices]

    it("contains several records") {
      whenReady(conn.run(nameStringIndices.size.result)) { res => res should be > 1 }
    }

    describe("#dataSourceId") {
      it("returns an id of a data source") {
        whenReady(conn.run(nameStringIndices
          .map { _.dataSourceId }.result.head)) { res =>
          res shouldBe an[Integer]
        }
      }
    }

    describe("#nameStringId") {
      it("returns an id of a name string") {
        whenReady(conn.run(nameStringIndices
          .map { _.nameStringId }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }

    describe("#url") {
      describe("CONTEXT: url is given") {
        it("returns url link to the index data") {
          whenReady(conn.run(nameStringIndices
            .filter(!_.url.isEmpty)
            .map { _.url }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }

      describe("CONTEXT: url is missing") {
        it("returns None") {
          whenReady(conn.run(nameStringIndices
            .filter(_.url.isEmpty)
            .map { _.url }.result.head)) { res =>
            res shouldBe None
          }
        }
      }
    }
  }
}
