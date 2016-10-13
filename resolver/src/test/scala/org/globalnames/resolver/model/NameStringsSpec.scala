package org.globalnames
package resolver
package model

import java.util.UUID

import slick.driver.PostgresDriver.api._

class NameStringsSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("NameStrings") {
    val nameStrings = TableQuery[NameStrings]

    it("contains several records") {
      whenReady(conn.run(nameStrings.size.result)) { res => res should be > 1 }
    }

    describe("#id") {
      it("returns an id of a name string") {
        whenReady(conn.run(nameStrings
          .map { _.id }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }

    describe("#name") {
      it("returns a name of a name string") {
        whenReady(conn.run(nameStrings
          .map { _.name }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#canonicalUuid") {
      describe("CONTEXT: canonical is given") {
        it("returns an optional canonicalUuid of a name string") {
          whenReady(conn.run(nameStrings
            .filter(!_.canonical.isEmpty)
            .map { _.canonicalUuid }.result.head)) { res =>
            res.value shouldBe an[UUID]
          }
        }
      }

      describe("CONTEXT: canonical is missing") {
        it("returns None") {
          whenReady(conn.run(nameStrings
            .filter { _.canonical.isEmpty }.map { _.canonicalUuid }.result.head)) { res =>
            res shouldBe None
          }
        }
      }
    }

    describe("#canonical") {
      describe("CONTEXT: canonical is known") {
        it("returns an optional canonical of a name string") {
          whenReady(conn.run(nameStrings
            .filter(!_.canonical.isEmpty)
            .map { _.canonical }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }

      describe("CONTEXT: canonical is missing") {
        it("returns None") {
          whenReady(conn.run(a = nameStrings
            .filter { _.canonical.isEmpty }.map { _.canonical }.result.head)) { res =>
            res shouldBe None
          }
        }
      }
    }
  }
}

