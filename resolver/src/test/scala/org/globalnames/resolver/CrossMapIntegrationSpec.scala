package org.globalnames
package resolver

import model.db.NameStrings
import slick.jdbc.PostgresProfile.api._

import scalaz._
import Scalaz._

class CrossMapIntegrationSpec extends SpecConfig {
  import CrossMapSearcher._

  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val crossMap = new CrossMapSearcher(conn)

  val db1Id = 4
  val db2Id = 11
  val dbSink = 179

  seed("test_resolver", "CrossMapIntegrationSpec")

  describe("CrossMap") {
    it("cross-maps relations when all parameters provided") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db2Id.some, Seq("40800", "65842"))) { res =>
        res.size shouldBe 2
        val Seq(res1, res2) = res
        res1.source shouldBe Source(db1Id, "40800")
        res2.source shouldBe Source(db1Id, "65842")
        res1.target should contain only Target(dbSink, db2Id, "5371640")
        res2.target should contain only (
          Target(dbSink, db2Id, "29"),
          Target(dbSink, db2Id, "302")
        )
      }
    }

    it("cross-maps without duplicates when sink is doubled") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink, dbSink), db2Id.some, Seq("40800"))) { res =>
        res.size shouldBe 1
        val Seq(res1) = res
        res1.source shouldBe Source(db1Id, "40800")
        res1.target should contain only Target(dbSink, db2Id, "5371640")
      }
    }

    it("cross-maps relations when no target id is provided") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), None, Seq("40800"))) { res =>
        res should have size 1
        res.head.source shouldBe Source(db1Id, "40800")
        res.head.target should contain only (
          Target(dbSink, 8, "10198787"),
          Target(dbSink, db2Id, "5371640")
        )
      }
    }

    it("cross-maps double local-ids") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db2Id.some, Seq("40800", "40800"))) { res =>
        res.size shouldBe 2
        res.head shouldBe res(1)
        res.head.source shouldBe Source(db1Id, "40800")
        res.head.target should contain only (
          Target(dbSink, 8, "10198787"),
          Target(dbSink, db2Id, "5371640")
        )
      }
    }

    it("cross-maps when source and target are switched") {
      whenReady(crossMap.execute(db2Id, Seq(dbSink), db1Id.some, Seq("5371640"))) { res =>
        res should have size 1
        res.head.source shouldBe Source(db2Id, "5371640")
        res.head.target should contain only Target(dbSink, db1Id, "40800")
      }
    }

    it("cross-maps nothing when: identity source/target and non-existing local ids") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db1Id.some,
                                 Seq("40800", "foobar"))) { res =>
        res.size shouldBe 2
        val Seq(res1, res2) = res
        res1.source shouldBe Source(db1Id, "40800")
        res2.source shouldBe Source(db1Id, "foobar")
        res1.target shouldBe empty
        res2.target shouldBe empty
      }
    }

    it("cross-maps nothing when no local ids are provided") {
      whenReady(crossMap.execute(db1Id, Seq(dbSink), db1Id.some, Seq())) { _ shouldBe empty }
      whenReady(crossMap.execute(db1Id, Seq(dbSink), None, Seq())) { _ shouldBe empty }
      whenReady(crossMap.execute(db1Id, Seq(), db2Id.some, Seq())) { _ shouldBe empty }
      whenReady(crossMap.execute(db1Id, Seq(), None, Seq())) { _ shouldBe empty }
    }

    it("cross-maps nothing on either DB ID is negative") {
      whenReady(crossMap.execute(-1, Seq(dbSink), db2Id.some, Seq("40800"))) { res =>
        res should have size 1
        res.head.target shouldBe empty
      }
      whenReady(crossMap.execute(-1, Seq(), db2Id.some, Seq("40800"))) { res =>
        res should have size 1
        res.head.target shouldBe empty
      }
      whenReady(crossMap.execute(db1Id, Seq(dbSink), (-1).some, Seq("40800"))) { res =>
        res should have size 1
        res.head.target shouldBe empty
      }
      whenReady(crossMap.execute(db1Id, Seq(), (-1).some, Seq("40800"))) { res =>
        res should have size 1
        res.head.target shouldBe empty
      }
      whenReady(crossMap.execute(db1Id, Seq(-1), db2Id.some, Seq("40800"))) { res =>
        res should have size 1
        res.head.target shouldBe empty
      }
    }
  }
}
