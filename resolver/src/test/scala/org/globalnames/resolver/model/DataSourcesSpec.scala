package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

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
        whenReady(conn.run(dataSources.take(1).map { _.id }.result)) { res =>
          res(0) shouldBe 1
        }
      }
    }

    describe("#title") {
      it("returns a title of a data source") {
        whenReady(conn.run(dataSources.take(1).map { _.title }.result)) { res =>
          res(0) shouldBe "Catalogue of Life"
        }
      }
    }

    describe("#description") {
      it("returns a description of a data source") {
        whenReady(conn.run(dataSources.take(1).map { _.description }.result)) { res =>
          res(0) should startWith ("[\"This release")
        }
      }
    }
  }
}
