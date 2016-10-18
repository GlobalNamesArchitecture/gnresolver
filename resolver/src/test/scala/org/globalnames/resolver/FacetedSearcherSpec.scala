package org.globalnames
package resolver

import java.util.UUID

import model.{Match, Matches, NameStrings}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class FacetedSearcherSpec extends SpecConfig {
  import Searcher._

  val conn = Database.forConfig("postgresql-test")
  val nameStrings = TableQuery[NameStrings]
  val faceted = new FacetedSearcher(conn)
  val searcher = new Searcher(mock[Resolver], faceted)

  seed("test_resolver", "FacetedSearcherSpec")

  val (takeDefault, dropDefault) = (50, 0)

  val Seq(ns05375e93f74c5bf488151cc363c1b98c,
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
        whenReady(searcher.resolve("Aaadonta constricta", CanonicalModifier(false))) {
          res =>
            res.matches should have size 3
            res.matches should contain theSameElementsAs Seq(
              Match(ns66d68908fe7d524b87ec86e5447993a0),
              Match(nsb2cf575fec5350ec96b4da94de2d926f),
              Match(nse529d9786a13578bb3ebbd9b8ad50a53)
            )
        }
      }

      it("resolves exact with multiple spaces input") {
        whenReady(searcher.resolve(" \t Aaadonta   constricta    ",
                                   CanonicalModifier(false))) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns66d68908fe7d524b87ec86e5447993a0),
            Match(nsb2cf575fec5350ec96b4da94de2d926f),
            Match(nse529d9786a13578bb3ebbd9b8ad50a53)
          )
        }
      }

      it("resolves exact with wildcard inside input") {
        whenReady(searcher.resolve("Aaadonta %constricta", CanonicalModifier(false))) {
          res => res.matches shouldBe empty
        }
      }

      it("resolves exact with wildcard as input") {
        whenReady(searcher.resolve("%", CanonicalModifier(false))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves wildcard") {
        whenReady(searcher.resolve("Aaadonta constricta ba", CanonicalModifier(true))) { res =>
            res.matches should have size 2
            res.matches should contain theSameElementsAs Seq(
              Match(ns5a68f4ec6121553e88433d602089ec88),
              Match(ns073bab6018165b5cb01887b4193db6f7)
            )
        }
      }

      it("resolves no mathches when string request of length less than 4 is provided") {
        val query = "Aaa"
        whenReady(searcher.resolve(query, CanonicalModifier(true))) { res =>
          res shouldBe Matches.empty(query + "%")
        }
      }

      it("returns no wildcarded matches when empty string is provided") {
        whenReady(searcher.resolve("", CanonicalModifier(true))) { res =>
          res shouldBe Matches.empty("%")
        }
      }
    }

    describe(".resolveAuthor") {
      it("resolves") {
        whenReady(searcher.resolve("Semper", AuthorModifier)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns66d68908fe7d524b87ec86e5447993a0),
            Match(nsb2cf575fec5350ec96b4da94de2d926f)
          )
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", AuthorModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveYear") {
      it("resolves") {
        whenReady(searcher.resolve("1874", YearModifier)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns66d68908fe7d524b87ec86e5447993a0),
            Match(nsb2cf575fec5350ec96b4da94de2d926f)
          )
        }
      }

      it("returns no matches on non-existing year") {
        whenReady(searcher.resolve("3000", YearModifier)) { res =>
          res.matches shouldBe empty
        }
      }

      it("returns no matches when empty string is provided 1") {
        whenReady(searcher.resolve("", YearModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveUninomial") {
      it("resolves") {
        whenReady(searcher.resolve("Aalenirhynchia", UninomialModifier)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns05375e93f74c5bf488151cc363c1b98c),
            Match(ns14414d49b3215aa39da132ca0ba45614),
            Match(nsc96fd1c5c5cb50edafd163bd1368896b)
          )
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", UninomialModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveGenus") {
      it("resolves") {
        whenReady(searcher.resolve("Aabacharis", GenusModifier)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(nsaebf444c6b645e338e6599f866538ecd),
            Match(ns7e38ca7a0c955c73b4d4a7c8b5efcb99),
            Match(ns3b963c1cc1265fc1998df42c4210216a)
          )
        }
      }

      it("resolves lowercase") {
        whenReady(searcher.resolve("aalenirhynchia", UninomialModifier)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns05375e93f74c5bf488151cc363c1b98c),
            Match(ns14414d49b3215aa39da132ca0ba45614),
            Match(nsc96fd1c5c5cb50edafd163bd1368896b)
          )
        }
      }

      it("resolves binomial") {
        whenReady(searcher.resolve("Aalenirhynchia ab", UninomialModifier)) { res =>
          res.matches shouldBe empty
        }
      }

      it("returns no matches on non-existing input") {
        whenReady(searcher.resolve("Aalenirhynchia1", UninomialModifier)) { res =>
          res.matches shouldBe empty
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", GenusModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveSpecies") {
      it("resolves") {
        whenReady(searcher.resolve("hansoni", SpeciesModifier)) { res =>
          res.matches should have size 3
          res.matches should contain theSameElementsAs Seq(
            Match(ns3b963c1cc1265fc1998df42c4210216a),
            Match(ns7e38ca7a0c955c73b4d4a7c8b5efcb99),
            Match(nsaebf444c6b645e338e6599f866538ecd)
          )
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", SpeciesModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveSubspecies") {
      it("resolves") {
        whenReady(searcher.resolve("Abacarioides", SubspeciesModifier)) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(nsa3a97b4b8c845da28feddab445c82d12),
            Match(nsf8a825d4b30e5234805d96dbb5230c87)
          )
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", SubspeciesModifier)) { res =>
          res.matches shouldBe empty
        }
      }
    }

    describe(".resolveNameStrings") {
      it("resolves exact") {
        whenReady(searcher.resolve("Aaadonta constricta babelthuapi",
                                   NameStringModifier(false))) { res =>
            res.matches should contain only Match(ns5a68f4ec6121553e88433d602089ec88)
        }
      }

      it("returns no matches when empty string is provided") {
        whenReady(searcher.resolve("", NameStringModifier(false))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves exact with non-existing input") {
        whenReady(searcher.resolve("Pararara", CanonicalModifier(false))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves no matches when empty string is provided") {
        whenReady(searcher.resolve("", CanonicalModifier(false))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves wildcard") {
        val query = "Aaadonta constricta komak"
        whenReady(searcher.resolve(query, NameStringModifier(true))) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns51b7b1b207ba5a0ea65dc5ca402b58de),
            Match(nsedd01cc80e7a53708d90173d24c9341c)
          )
          res.suppliedNameString shouldBe (query + "%")
        }
      }

      it("returns no wildcarded matches when empty string is provided") {
        whenReady(searcher.resolve("", NameStringModifier(true))) { res =>
          res shouldBe Matches.empty("%")
        }
      }

      it("resolves nothing wildcard with non-existing stirng") {
        whenReady(searcher.resolve("Pararara", CanonicalModifier(true))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves wildcard with wildcard in begin of input") {
        whenReady(searcher.resolve("%Aaadonta constricta ba", CanonicalModifier(true))) { res =>
          res.matches should have size 2
          res.matches should contain theSameElementsAs Seq(
            Match(ns073bab6018165b5cb01887b4193db6f7),
            Match(ns5a68f4ec6121553e88433d602089ec88)
          )
        }
      }

      it("resolves wildcard with wildcard in middle of input") {
        whenReady(searcher.resolve("Aaadonta % constricta ba", CanonicalModifier(true))) { res =>
          res.matches shouldBe empty
        }
      }

      it("resolves no matches when string request of length less than 4 is provided") {
        val query = "Aaa"
        whenReady(searcher.resolve(query, NameStringModifier(true))) { res =>
          res shouldBe Matches.empty(query + "%")
        }
      }
    }

    describe(".resolveExact") {
      it("resolves") {
        whenReady(searcher.resolve("Aalenirhynchia", ExactModifier)) { res =>
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
        whenReady(faceted.resolveDataSources(
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
        whenReady(faceted.findNameStringByUuid(
          UUID.fromString("073bab60-1816-5b5c-b018-87b4193db6f7"))) { res =>
            res.matches should contain only Match(ns073bab6018165b5cb01887b4193db6f7)
        }
      }
    }
  }
}
