package org.globalnames
package resolver

import model.NameStrings
import slick.driver.PostgresDriver.api._

class CrossMapIntegrationSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val crossMap = new CrossMap(conn)

  val dbSourceId = 8
  val dbTargetId = 178

  seed("test_resolver", "CrossMapIntegrationSpec")

  describe("CrossMap") {
    it("finds cross-map relations") {
      whenReady(crossMap.execute(dbSourceId, dbTargetId, Seq("1", "1005101"))) { res =>
        res.size shouldBe 2
        res should contain theSameElementsAs Seq(("1", ""), ("1005101", "AF468436/#6"))
      }
    }

    it("finds nothing when no local ids are provided") {
      whenReady(crossMap.execute(dbSourceId, dbTargetId, Seq())) { _ shouldBe empty }
    }

    it("finds nothing on either DB ID is negative") {
      whenReady(crossMap.execute(-1, dbTargetId, Seq())) { _ shouldBe empty }
      whenReady(crossMap.execute(dbSourceId, -1, Seq())) { _ shouldBe empty }
      whenReady(crossMap.execute(-1, dbTargetId, Seq("1"))) { _ shouldBe Seq(("1", "")) }
      whenReady(crossMap.execute(dbSourceId, -1, Seq("1"))) { _ shouldBe Seq(("1", "")) }
    }

    it("finds nothing on non-existing DB ID") {
      val absentId = 1000000
      whenReady(crossMap.execute(dbSourceId, absentId, Seq("1"))) { _ shouldBe Seq(("1", "")) }
      whenReady(crossMap.execute(absentId, dbTargetId, Seq("1"))) { _ shouldBe Seq(("1", "")) }
    }
  }
}
