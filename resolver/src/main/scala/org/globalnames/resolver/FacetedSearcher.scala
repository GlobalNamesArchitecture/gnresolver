package org.globalnames
package resolver

import java.util.UUID

import resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Success, Try}

class FacetedSearcher(val db: Database) extends Materializer {
  import Materializer.Parameters

  private val unaccent = SimpleFunction.unary[String, String]("unaccent")

  private val gen = UuidGenerator()

  private[resolver] def resolveCanonical(canonicalName: String) = {
    val canonicalNameUuid = gen.generate(canonicalName)
    nameStrings.filter { ns =>
      ns.canonicalUuid =!= NameStrings.emptyCanonicalUuid && ns.canonicalUuid === canonicalNameUuid
    }
  }

  private[resolver] def resolveCanonicalLike(canonicalName: String) = {
    if (canonicalName.length <= 3) {
      nameStrings.take(0)
    } else {
      val canonicalNameLike = canonicalName + "%"
      nameStrings.filter { x => x.canonical.like(canonicalNameLike) }
    }
  }

  private[resolver] def resolveAuthor(authorName: String) = {
    val query = authorWords.filter { aw => aw.authorWord === unaccent(authorName.toUpperCase) }
                           .map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveAuthorWildcard(authorName: String) = {
    if (authorName.isEmpty) {
      nameStrings.take(0)
    } else {
      val authorNameLike = authorName + "%"
      val query = authorWords.filter { aw =>
        aw.authorWord.like(unaccent(authorNameLike.toUpperCase))
      }.map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveYear(year: String) = {
    Try(year.toInt) match {
      case Success(yr) if yr < 1758 =>
        nameStrings.take(0)
      case _ =>
        val query = yearWords.filter { x => x.yearWord === year }.map { _.nameStringUuid }
        nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveYearWildcard(year: String) = {
    if (year.isEmpty) {
      nameStrings.take(0)
    } else {
      val yearLike = year + "%"
      val query = yearWords.filter { yw => yw.yearWord.like(yearLike) }
                           .map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveUninomial(uninomial: String) = {
    val query = uninomialWords.filter { uw =>
      uw.uninomialWord === unaccent(uninomial.toUpperCase)
    }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveUninomialWildcard(uninomial: String) = {
    if (uninomial.length <= 3) {
      nameStrings.take(0)
    } else {
      val uninomialLike = uninomial + "%"
      val query = uninomialWords.filter { uw =>
        uw.uninomialWord.like(unaccent(uninomialLike.toUpperCase))
      }.map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveGenus(genus: String) = {
    val query = genusWords.filter { uw =>
      uw.genusWord === unaccent(genus.toUpperCase)
    }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveGenusWildcard(genus: String) = {
    if (genus.length <= 3) {
      nameStrings.take(0)
    } else {
      val genusLike = genus + "%"
      val query = genusWords.filter { uw =>
        uw.genusWord.like(unaccent(genusLike.toUpperCase))
      }.map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveSpecies(species: String) = {
    val query = speciesWords.filter { sw =>
      sw.speciesWord === unaccent(species.toUpperCase)
    }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveSpeciesWildcard(species: String) = {
    if (species.length <= 3) {
      nameStrings.take(0)
    } else {
      val speciesLike = species + "%"
      val query = speciesWords.filter { sw =>
        sw.speciesWord.like(unaccent(speciesLike.toUpperCase))
      }.map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveSubspecies(subspecies: String) = {
    val query = subspeciesWords.filter { ssw =>
      ssw.subspeciesWord === unaccent(subspecies.toUpperCase)
    }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveSubspeciesWildcard(subspecies: String) = {
    if (subspecies.length <= 3) {
      nameStrings.take(0)
    } else {
      val subspeciesLike = subspecies + "%"
      val query = subspeciesWords.filter { ssw =>
        ssw.subspeciesWord.like(unaccent(subspeciesLike.toUpperCase))
      }.map { _.nameStringUuid }
      nameStrings.filter { ns => ns.id.in(query) }
    }
  }

  private[resolver] def resolveNameStrings(nameStringQuery: String) = {
    nameStrings.filter { ns => unaccent(ns.name) === unaccent(nameStringQuery) }
  }

  private[resolver] def resolveNameStringsLike(nameStringQuery: String) = {
    if (nameStringQuery.length <= 3) {
      nameStrings.take(0)
    } else {
      val nameStringQueryLike = nameStringQuery + "%"
      nameStrings.filter { ns => unaccent(ns.name).like(unaccent(nameStringQueryLike)) }
    }
  }

  private[resolver] def resolveExact(exact: String) = {
    val exactUuid = gen.generate(exact)
    nameStrings.filter { ns => ns.id === exactUuid }
  }

  def resolveDataSources(uuid: UUID): Future[Seq[(NameStringIndex, DataSource)]] = {
    val query = for {
      nsi <- nameStringIndicies
      ds <- dataSources
      if nsi.nameStringId === uuid && nsi.dataSourceId === ds.id
    } yield (nsi, ds)

    db.run(query.result)
  }

  def findNameStringByUuid(uuid: UUID, parameters: Parameters): Future[Matches] = {
    val params = parameters.copy(query = None, matchType = MatchType.UUIDLookup)
    nameStringsMatches(nameStrings.filter { ns => ns.id === uuid }, params)
  }
}
