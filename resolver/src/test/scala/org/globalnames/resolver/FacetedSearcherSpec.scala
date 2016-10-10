package org.globalnames
package resolver

import java.util.UUID

import model.{Match, NameStrings}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class FacetedSearcherSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val searcher = new FacetedSearcher(conn)

  seed("test_resolver", "FacetedSearcherSpec")

  val (takeDefault, dropDefault) = (50, 0)

  val all @ Seq(ns05375e93f74c5bf488151cc363c1b98c,
                ns073bab6018165b5cb01887b4193db6f7,
                ns14414d49b3215aa39da132ca0ba45614,
                ns3b963c1cc1265fc1998df42c4210216a,
                ns51b7b1b207ba5a0ea65dc5ca402b58de,
                ns5a68f4ec6121553e88433d602089ec88,
                ns66d68908fe7d524b87ec86e5447993a0,
                ns7c9866391fd55277987ce18f3301388a,
                ns7e38ca7a0c955c73b4d4a7c8b5efcb99,
                nsa3a97b4b8c845da28feddab445c82d12,
                nsaebf444c6b645e338e6599f866538ecd,
                nsb2cf575fec5350ec96b4da94de2d926f,
                nsc96fd1c5c5cb50edafd163bd1368896b,
                nsd0cf534d0785576b87a8960e5e6ce374,
                nsdc6e0eb5363254aaaa16bfa8de8b92db,
                nse529d9786a13578bb3ebbd9b8ad50a53,
                nsedd01cc80e7a53708d90173d24c9341c,
                nsf8a825d4b30e5234805d96dbb5230c87) =
    Await.result(conn.run(nameStrings.sortBy { _.id }.result), 3.seconds)

  describe("FacetedSearcher") {
    describe(".resolveCanonical") {
      it("resolves exact") {
        whenReady(searcher.resolveCanonical("Aaadonta constricta", takeDefault, dropDefault)) {
          res =>
            res.matches should have size 3
            res.matches should contain theSameElementsAs Seq(
              Match(ns66d68908fe7d524b87ec86e5447993a0),
              Match(nsb2cf575fec5350ec96b4da94de2d926f),
              Match(nse529d9786a13578bb3ebbd9b8ad50a53)
            )
        }
      }

      it("resolves wildcard") {
        whenReady(searcher.resolveCanonicalLike("Aaadonta constricta ba%",
          takeDefault, dropDefault)) { res =>
            res.matches should have size 2
            res.matches should contain theSameElementsAs Seq(
              Match(ns5a68f4ec6121553e88433d602089ec88),
              Match(ns073bab6018165b5cb01887b4193db6f7)
            )
        }
      }
    }

    describe(".resolveAuthor") {
      it("resolves") {
        whenReady(searcher.resolveAuthor("Semper", takeDefault, dropDefault)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns66d68908fe7d524b87ec86e5447993a0),
            Match(nsb2cf575fec5350ec96b4da94de2d926f)
          )
        }
      }
    }

    describe(".resolveYear") {
      it("resolves") {
        whenReady(searcher.resolveYear("1874", takeDefault, dropDefault)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns66d68908fe7d524b87ec86e5447993a0),
            Match(nsb2cf575fec5350ec96b4da94de2d926f)
          )
        }
      }
    }

    describe(".resolveUninomial") {
      it("resolves") {
        whenReady(searcher.resolveUninomial("Aalenirhynchia", takeDefault, dropDefault)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns05375e93f74c5bf488151cc363c1b98c),
            Match(ns14414d49b3215aa39da132ca0ba45614),
            Match(nsc96fd1c5c5cb50edafd163bd1368896b)
          )
        }
      }
    }

    describe(".resolveGenus") {
      it("resolves") {
        whenReady(searcher.resolveGenus("Aabacharis", takeDefault, dropDefault)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(nsaebf444c6b645e338e6599f866538ecd),
            Match(ns7e38ca7a0c955c73b4d4a7c8b5efcb99),
            Match(ns3b963c1cc1265fc1998df42c4210216a)
          )
        }
      }
    }

    describe(".resolveSpecies") {
      it("resolves") {
        whenReady(searcher.resolveSpecies("hansoni", takeDefault, dropDefault)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns3b963c1cc1265fc1998df42c4210216a),
            Match(ns7e38ca7a0c955c73b4d4a7c8b5efcb99),
            Match(nsaebf444c6b645e338e6599f866538ecd)
          )
        }
      }
    }

    describe(".resolveSubspecies") {
      it("resolves") {
        whenReady(searcher.resolveSubspecies("Abacarioides", takeDefault, dropDefault)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(nsa3a97b4b8c845da28feddab445c82d12),
            Match(nsf8a825d4b30e5234805d96dbb5230c87)
          )
        }
      }
    }

    describe(".resolveNameStrings") {
      it("resolves exact") {
        whenReady(searcher.resolveNameStrings("Aaadonta constricta babelthuapi",
          takeDefault, dropDefault)) { res =>
            res.matches should contain only Match(ns5a68f4ec6121553e88433d602089ec88)
        }
      }

      it("resolves wildcard") {
        whenReady(searcher.resolveNameStringsLike("Aaadonta constricta komak%",
          takeDefault, dropDefault)) { res =>
            res.matches should have size 2
            res.matches should contain theSameElementsAs Seq(
              Match(ns51b7b1b207ba5a0ea65dc5ca402b58de),
              Match(nsedd01cc80e7a53708d90173d24c9341c)
            )
        }
      }
    }

    describe(".resolveExact") {
      it("resolves") {
        whenReady(searcher.resolveExact("Aalenirhynchia", takeDefault, dropDefault)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns05375e93f74c5bf488151cc363c1b98c),
            Match(ns14414d49b3215aa39da132ca0ba45614),
            Match(nsc96fd1c5c5cb50edafd163bd1368896b)
          )
        }
      }
    }

    describe(".resolveDataSources") {
      it("resolves") {
        whenReady(searcher.resolveDataSources(
          UUID.fromString("b2cf575f-ec53-50ec-96b4-da94de2d926f"))) { res =>
          res should have size 2
          val res1 = res.map { case (nsi, ds) => (nsi.nameStringId, ds.id) }
          res1 should contain theSameElementsAs Seq(
            (UUID.fromString("b2cf575f-ec53-50ec-96b4-da94de2d926f"), 168),
            (UUID.fromString("b2cf575f-ec53-50ec-96b4-da94de2d926f"), 169))
        }
      }
    }

    describe(".findNameStringByUuid") {
      it("resolves") {
        whenReady(searcher.findNameStringByUuid(
          UUID.fromString("073bab60-1816-5b5c-b018-87b4193db6f7"))) { res =>
            res.matches should contain only Match(ns073bab6018165b5cb01887b4193db6f7)
        }
      }
    }
  }
}
