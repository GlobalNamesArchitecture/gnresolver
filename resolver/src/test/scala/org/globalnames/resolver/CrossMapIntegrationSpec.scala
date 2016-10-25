package org.globalnames
package resolver

import model.NameStrings
import slick.driver.PostgresDriver.api._

class CrossMapIntegrationSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val crossMap = new CrossMap(conn)

  val db1Id = 4
  val db2Id = 11
  val dbSink = 179

  seed("test_resolver", "CrossMapIntegrationSpec")

  describe("CrossMap") {
    it("cross-maps relations when all parameters provided") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db2Id, Seq("40800"), Seq("745315"))) { res =>
        res.size shouldBe 1
        res shouldBe Seq(("40800", "5371640"))
      }
    }

    it("cross-maps when source and target are switched") {
      whenReady(crossMap.execute(db2Id, Seq(dbSink), db1Id, Seq("5371640"), Seq("745315"))) { res =>
        res.size shouldBe 1
        res shouldBe Seq(("5371640", "40800"))
      }
    }

    it("cross-maps identity source/target and omitting non-existing taxons") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db1Id,
                                 Seq("40800", "foobar"), Seq("745315"))) { res =>
        res.size shouldBe 2
        res should contain only (("40800", "40800"), ("foobar", ""))
      }
    }

    it("cross-maps nothing when no local ids are provided") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db2Id,
                                 Seq("40800"), Seq())) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db1Id,
                                 Seq("40800"), Seq())) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(), db2Id,
                                 Seq("40800"), Seq())) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(), db1Id,
                                 Seq("40800"), Seq())) { _ shouldBe Seq(("40800", "")) }
    }

    it("finds nothing on either DB ID is negative") {
      whenReady(crossMap.execute(-1, Seq(dbSink), db2Id,
                                 Seq("40800"), Seq("745315"))) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(-1, Seq(), db2Id,
                                 Seq("40800"), Seq("745315"))) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(dbSink), -1,
                                 Seq("40800"), Seq("745315"))) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(), -1,
                                 Seq("40800"), Seq("745315"))) { _ shouldBe Seq(("40800", "")) }
      whenReady(crossMap.execute(db1Id, Seq(-1), db2Id,
                                 Seq("40800"), Seq("745315"))) { _ shouldBe Seq(("40800", "")) }
    }

  }
}
