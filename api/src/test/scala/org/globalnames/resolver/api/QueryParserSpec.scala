package org.globalnames.resolver.api

import org.scalatest.FunSpec
import org.globalnames.resolver.api.QueryParser.{Modifier, SearchPart}

class QueryParserSpec extends FunSpec {
  val context = describe _

  describe(".result") {
    context("modifiers") {
      it("works without modifier") {
        assert(QueryParser.result("Pomatomus") ==
          SearchPart(Modifier("none"), "Pomatomus", false))
      }

      it("recognises 'exact' modifier") {
        assert(QueryParser.result("exact:Pomatomus") ==
          SearchPart(Modifier("exact"), "Pomatomus", false))
      }

      it("recognises 'name string' modifier"){
        assert(QueryParser.result("ns:Pomatomus") ==
          SearchPart(Modifier("ns"), "Pomatomus", false))
      }

      it("recognises 'canonical form' modifier") {
        assert(QueryParser.result("can:Pomatomus") ==
          SearchPart(Modifier("can"), "Pomatomus", false))
      }

      it("recognises 'uninomial' modifier") {
        assert(QueryParser.result("uni:Pomatomus") ==
          SearchPart(Modifier("uni"), "Pomatomus", false))
      }

      it("recognises 'genus' modifier") {
        assert(QueryParser.result("gen:Pomatomus") ==
          SearchPart(Modifier("gen"), "Pomatomus", false))
      }

      it("recognises 'species' modifier") {
        assert(QueryParser.result("sp:saltator") ==
          SearchPart(Modifier("sp"), "saltator", false))
      }

      it("recognises 'infraspecies' modifier") {
        assert(QueryParser.result("ssp:sapiens") ==
          SearchPart(Modifier("ssp"), "sapiens", false))
      }

      it("recognises 'author' modifier") {
        assert(QueryParser.result("au:Linn") ==
          SearchPart(Modifier("au"), "Linn", false))
      }

      it("recognises 'year' modifier") {
        assert(QueryParser.result("yr:1889") ==
          SearchPart(Modifier("yr"), "1889", false))
      }
    }

    context("wild card") {
      it("recognises wild card without modifiers") {
        assert(QueryParser.result("Pom*") ==
          SearchPart(Modifier("none"), "Pom", true))
      }

      it("recognises wild card with modifiers") {
        assert(QueryParser.result("gen:Pom*") ==
          SearchPart(Modifier("gen"), "Pom", true))
      }
    }

    context("edge cases") {
      it("ignores unknown modifiers") {
        assert(QueryParser.result("what:Something") ==
          SearchPart(Modifier("none"), "what:Something", false))
      }

      it("ignores missing modifier") {
        assert(QueryParser.result(":Something") ==
          SearchPart(Modifier("none"), ":Something", false))
      }

      it("ignores missplaced modifier") {
        assert(QueryParser.result("Something:ssp") ==
          SearchPart(Modifier("none"), "Something:ssp", false))
      }

      it("ignores chained modifiers") {
        assert(QueryParser.result("yr:sp:1887") ==
          SearchPart(Modifier("none"), "yr:sp:1887", false))
      }

      it("recognises wildcard ignoring bad modifiers") {
        assert(QueryParser.result("yr:sp:1887*") ==
          SearchPart(Modifier("none"), "yr:sp:1887", true))
      }
    }

    context("BUGS") {
      it("is confused by text starting as the same modifier") {
        assert(QueryParser.result("sp:spathulata") ==
          SearchPart(Modifier("none"), "sp:spathulata", false))
      }

      it("is confused by text starting as a different modifier") {
        assert(QueryParser.result("au:spathulata") ==
          SearchPart(Modifier("none"), "au:spathulata", false))
      }

      it("ignores modifier repeated without text") {
        assert(QueryParser.result("yr:yr") ==
          SearchPart(Modifier("none"), "yr:yr", false))
      }
    }
  }
}
