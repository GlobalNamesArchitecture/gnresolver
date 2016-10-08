package org.globalnames
package resolver
package api

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import QueryParser.{Modifier, SearchPart}

class QueryParserSpec extends FunSpec {
  describe(".result") {
    describe("modifiers") {
      it("works without modifier") {
        QueryParser.result("Pomatomus") shouldBe
          SearchPart(Modifier("none"), "Pomatomus", false)
      }

      it("recognises 'exact' modifier") {
        QueryParser.result("exact:Pomatomus") shouldBe
          SearchPart(Modifier("exact"), "Pomatomus", false)
      }

      it("recognises 'name string' modifier"){
        QueryParser.result("ns:Pomatomus") shouldBe
          SearchPart(Modifier("ns"), "Pomatomus", false)
      }

      it("recognises 'canonical form' modifier") {
        QueryParser.result("can:Pomatomus") shouldBe
          SearchPart(Modifier("can"), "Pomatomus", false)
      }

      it("recognises 'uninomial' modifier") {
        QueryParser.result("uni:Pomatomus") shouldBe
          SearchPart(Modifier("uni"), "Pomatomus", false)
      }

      it("recognises 'genus' modifier") {
        QueryParser.result("gen:Pomatomus") shouldBe
          SearchPart(Modifier("gen"), "Pomatomus", false)
      }

      it("recognises 'species' modifier") {
        QueryParser.result("sp:saltator") shouldBe
          SearchPart(Modifier("sp"), "saltator", false)
      }

      it("recognises 'infraspecies' modifier") {
        QueryParser.result("ssp:sapiens") shouldBe
          SearchPart(Modifier("ssp"), "sapiens", false)
      }

      it("recognises 'author' modifier") {
        QueryParser.result("au:Linn") shouldBe
          SearchPart(Modifier("au"), "Linn", false)
      }

      it("recognises 'year' modifier") {
        QueryParser.result("yr:1889") shouldBe
          SearchPart(Modifier("yr"), "1889", false)
      }
    }

    describe("wild card") {
      it("recognises wild card without modifiers") {
        QueryParser.result("Pom*") shouldBe
          SearchPart(Modifier("none"), "Pom", true)
      }

      it("recognises wild card with modifiers") {
        QueryParser.result("gen:Pom*") shouldBe
          SearchPart(Modifier("gen"), "Pom", true)
      }
    }

    describe("edge cases") {
      it("ignores unknown modifiers") {
        QueryParser.result("what:Something") shouldBe
          SearchPart(Modifier("none"), "what:Something", false)
      }

      it("ignores missing modifier") {
        QueryParser.result(":Something") shouldBe
          SearchPart(Modifier("none"), ":Something", false)
      }

      it("ignores missplaced modifier") {
        QueryParser.result("Something:ssp") shouldBe
          SearchPart(Modifier("none"), "Something:ssp", false)
      }

      it("ignores chained modifiers") {
        QueryParser.result("yr:sp:1887") shouldBe
          SearchPart(Modifier("none"), "yr:sp:1887", false)
      }

      it("recognises wildcard ignoring bad modifiers") {
        QueryParser.result("yr:sp:1887*") shouldBe
          SearchPart(Modifier("none"), "yr:sp:1887", true)
      }
    }

    it("is confused by text starting as the same modifier") {
      pending
      QueryParser.result("sp:spathulata") shouldBe
        SearchPart(Modifier("none"), "sp:spathulata", false)
    }

    it("is confused by text starting as a different modifier") {
      pending
      QueryParser.result("au:spathulata") shouldBe
        SearchPart(Modifier("none"), "au:spathulata", false)
    }

    it("ignores modifier repeated without text") {
      pending
      QueryParser.result("yr:yr") shouldBe
        SearchPart(Modifier("none"), "yr:yr", false)
    }
  }
}
