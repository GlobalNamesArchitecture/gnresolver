package org.globalnames
package resolver
package model

import slick.driver.PostgresDriver.api._

import java.util.UUID

class NameStringIndicesSpec extends SpecConfig {
  val conn = Database.forConfig("postgresql-test")
  seed("test_resolver", "ResolverIntegrationSpec")

  override def afterAll(): Unit = {
    conn.close()
  }

  describe("NameStringIndices") {
    val nameStringIndices = TableQuery[NameStringIndices]

    it("contains several records") {
      whenReady(conn.run(nameStringIndices.size.result)) { res => res should be > 1 }
    }

    describe("#dataSourceId") {
      it("returns an id of a data source") {
        whenReady(conn.run(nameStringIndices.map { _.dataSourceId }.result.head)) { res =>
          res shouldBe an[Integer]
        }
      }
    }

    describe("#nameStringId") {
      it("returns an id of a name string") {
        whenReady(conn.run(nameStringIndices.map { _.nameStringId }.result.head)) { res =>
          res shouldBe an[UUID]
        }
      }
    }

    describe("#url") {
      describe("CONTEXT: url is given") {
        it("returns url link to the index data") {
          whenReady(conn.run(nameStringIndices.filter { _.url.isDefined }
                                              .map { _.url }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }

      describe("CONTEXT: url is missing") {
        it("returns None") {
          whenReady(conn.run(nameStringIndices.filter { _.url.isEmpty }
                                              .map { _.url }.result.head)) { res =>
            res shouldBe None
          }
        }
      }
    }

    describe("#taxonId") {
      it("returns an id of a taxon id") {
        whenReady(conn.run(nameStringIndices.map { _.taxonId }.result.head)) { res =>
          res shouldBe a[String]
        }
      }
    }

    describe("#globalId") {
      describe("CONTEXT: global_id is given") {
        it("returns an id of a global_id") {
          whenReady(conn.run(nameStringIndices.filter { _.globalId.isDefined }
                                              .map { _.globalId }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#localId") {
      describe("CONTEXT: local id is given") {
        it("returns an id of local_id") {
          whenReady(conn.run(nameStringIndices.filter { _.localId.isDefined }
                                              .map { _.localId }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#nomenclaturalCodeId") {
      describe("CONTEXT: nomenclatural_code_id is given") {
        it("returns an id of nomenclatural_code_id") {
          whenReady(conn.run(nameStringIndices.filter { _.nomenclaturalCodeId.isDefined }
                                              .map { _.nomenclaturalCodeId }.result.head)) {res =>
            res.value shouldBe a[Integer]
          }
        }
      }
    }

    describe("#rank") {
      describe("CONTEXT: rank is given") {
        it("returns an rank") {
          whenReady(conn.run(nameStringIndices.filter { _.rank.isDefined }
                                              .map { _.rank }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#acceptedTaxonId") {
      describe("CONTEXT: accepted_taxon_id is given") {
        it("returns an accepted_taxon_id") {
          whenReady(conn.run(nameStringIndices.filter { _.acceptedTaxonId.isDefined }
                                              .map { _.acceptedTaxonId }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#classificationPath") {
      describe("CONTEXT: classification_path is given") {
        it("returns a classification_path") {
          whenReady(conn.run(nameStringIndices.filter { _.classificationPath.isDefined }
                                              .map { _.classificationPath }.result.head)) { res =>
            res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#classificationPathIds") {
      describe("CONTEXT: classification_path_ids is given") {
        it("returns a classification_path_ids") {
          whenReady(conn.run(nameStringIndices.filter { _.classificationPathIds.isDefined }
                                              .map { _.classificationPathIds }.result.head)) {
            res => res.value shouldBe a[String]
          }
        }
      }
    }

    describe("#classificationPathRanks") {
      describe("CONTEXT: classification_path_ranks is given") {
        it("returns a classification_path_ranks") {
          whenReady(conn.run(nameStringIndices.filter { _.classificationPathRanks.isDefined }
                                              .map { _.classificationPathRanks }.result.head)) {
            res => res.value shouldBe a[String]
          }
        }
      }
    }
  }
}
