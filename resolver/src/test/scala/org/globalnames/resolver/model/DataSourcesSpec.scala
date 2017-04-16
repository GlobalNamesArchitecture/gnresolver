package org.globalnames
package resolver
package model
package db

import slick.jdbc.PostgresProfile.api._

class DataSourcesSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("DataSources") {
    val dataSources = TableQuery[DataSources]

    it("contains several records") {
      whenReady(conn.run(dataSources.size.result)) { res => res should be > 1 }
    }

    describe("#id") {
      it("returns an id of a data source") {
        whenReady(conn.run(dataSources.map { _.id }.result.head)) { res =>
          res shouldBe an[Integer]
        }
      }
    }

    describe("#title") {
      it("returns a title of a data source") {
        whenReady(conn.run(dataSources.map { _.title }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#description") {
      it("returns a description of a data source") {
        whenReady(conn.run(dataSources.map { _.description }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }
  }
}
