package org.globalnames
package resolver
package api

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import QueryParser.SearchPart
import Searcher._

class QueryParserSpec extends FunSpec {
  describe(".result") {
    describe("modifiers") {
      it("works without modifier") {
        QueryParser.result("Pomatomus") shouldBe
          SearchPart(NoModifier, "Pomatomus", false)
      }

      it("recognises 'exact' modifier") {
        QueryParser.result("exact:Pomatomus") shouldBe
          SearchPart(ExactModifier, "Pomatomus", false)
      }

      it("recognises 'name string' modifier") {
        QueryParser.result("ns:Pomatomus") shouldBe
          SearchPart(NameStringModifier, "Pomatomus", false)
      }

      it("recognises 'canonical form' modifier") {
        QueryParser.result("can:Pomatomus") shouldBe
          SearchPart(CanonicalModifier, "Pomatomus", false)
      }

      it("recognises 'uninomial' modifier") {
        QueryParser.result("uni:Pomatomus") shouldBe
          SearchPart(UninomialModifier, "Pomatomus", false)
      }

      it("recognises 'genus' modifier") {
        QueryParser.result("gen:Pomatomus") shouldBe
          SearchPart(GenusModifier, "Pomatomus", false)
      }

      it("recognises 'species' modifier") {
        QueryParser.result("sp:saltator") shouldBe
          SearchPart(SpeciesModifier, "saltator", false)
      }

      it("recognises 'infraspecies' modifier") {
        QueryParser.result("ssp:sapiens") shouldBe
          SearchPart(SubspeciesModifier, "sapiens", false)
      }

      it("recognises 'author' modifier") {
        QueryParser.result("au:Linn") shouldBe
          SearchPart(AuthorModifier, "Linn", false)
      }

      it("recognises 'year' modifier") {
        QueryParser.result("yr:1889") shouldBe
          SearchPart(YearModifier, "1889", false)
      }
    }

    describe("wild card") {
      it("recognises wild card without modifiers") {
        QueryParser.result("Pom*") shouldBe
          SearchPart(NoModifier, "Pom", true)
      }

      it("recognises wild card with modifiers") {
        QueryParser.result("gen:Pom*") shouldBe
          SearchPart(GenusModifier, "Pom", true)
      }
    }

    describe("edge cases") {
      it("ignores unknown modifiers") {
        QueryParser.result("what:Something") shouldBe
          SearchPart(NoModifier, "what:Something", false)
      }

      it("ignores missing modifier") {
        QueryParser.result(":Something") shouldBe
          SearchPart(NoModifier, ":Something", false)
      }

      it("ignores misplaced modifier") {
        QueryParser.result("Something:ssp") shouldBe
          SearchPart(NoModifier, "Something:ssp", false)
      }

      it("ignores chained modifiers") {
        QueryParser.result("yr:sp:1887") shouldBe
          SearchPart(YearModifier, "sp:1887", false)
      }

      it("recognises wildcard ignoring bad modifiers") {
        QueryParser.result("yr:sp:1887*") shouldBe
          SearchPart(YearModifier, "sp:1887", true)
      }

      it("recognizes text starting as the same modifier") {
        QueryParser.result("sp:spathulata") shouldBe
          SearchPart(SpeciesModifier, "spathulata", false)
      }

      it("recognizes text starting as a different modifier") {
        QueryParser.result("au:spathulata") shouldBe
          SearchPart(AuthorModifier, "spathulata", false)
      }

      it("ignores modifier repeated without text") {
        QueryParser.result("yr:yr") shouldBe
          SearchPart(YearModifier, "yr", false)
      }
    }
  }
}
