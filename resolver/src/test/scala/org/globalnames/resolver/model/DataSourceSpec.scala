package org.globalnames
package resolver
package modles

import org.globalnames.resolver.model.DataSources
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class DataSourceSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  describe("DataSource") {
    val dataSource = TableQuery[DataSources]

  }
  override def afterAll(): Unit = {
    conn.close()
  }
}
