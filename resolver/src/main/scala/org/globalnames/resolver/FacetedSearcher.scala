package org.globalnames
package resolver

import java.util.UUID

import resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future

class FacetedSearcher(val db: Database) extends Materializer {
  import Materializer.Parameters

  private val gen = UuidGenerator()

  private[resolver] def resolveCanonical(canonicalName: String) = {
    val canonicalNameUuid = gen.generate(canonicalName)
    nameStrings.filter { x => x.canonicalUuid === canonicalNameUuid }
  }

  private[resolver] def resolveCanonicalLike(canonicalName: String) = {
    val canonicalNameLike = canonicalName + "%"
    if (canonicalName.length <= 3) {
      nameStrings.take(0)
    } else {
      nameStrings.filter { x => x.canonical.like(canonicalNameLike) }
    }
  }

  private[resolver] def resolveAuthor(authorName: String) = {
    val query = authorWords.filter { x => x.authorWord === authorName }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveYear(year: String) = {
    val query = yearWords.filter { x => x.yearWord === year }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveUninomial(uninomial: String) = {
    val query = uninomialWords.filter { x => x.uninomialWord === uninomial }
                              .map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveGenus(genus: String) = {
    val query = genusWords.filter { x => x.genusWord === genus }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveSpecies(species: String) = {
    val query = speciesWords.filter { x => x.speciesWord === species }.map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveSubspecies(subspecies: String) = {
    val query = subspeciesWords.filter { x => x.subspeciesWord === subspecies }
                               .map { _.nameStringUuid }
    nameStrings.filter { ns => ns.id.in(query) }
  }

  private[resolver] def resolveNameStrings(nameStringQuery: String) = {
    nameStrings.filter { ns => ns.name === nameStringQuery }
  }

  private[resolver] def resolveNameStringsLike(nameStringQuery: String) = {
    val nameStringQueryLike = nameStringQuery + "%"
    if (nameStringQuery.length <= 3) {
      nameStrings.take(0)
    } else {
      nameStrings.filter { ns => ns.name.like(nameStringQueryLike) }
    }
  }

  private[resolver] def resolveExact(exact: String) = {
    nameStrings.filter { ns => ns.canonical === exact || ns.name === exact }
  }

  def resolveDataSources(uuid: UUID): Future[Seq[(NameStringIndex, DataSource)]] = {
    val query = for {
      nsi <- nameStringIndicies
      ds <- dataSources
      if nsi.nameStringId === uuid && nsi.dataSourceId === ds.id
    } yield (nsi, ds)

    db.run(query.result)
  }

  def findNameStringByUuid(uuid: UUID, withVernaculars: Boolean): Future[Matches] = {
    nameStringsMatches(nameStrings.filter { ns => ns.id === uuid },
                       Parameters(take = 1, drop = 0, withSurrogates = true,
                                  withVernaculars, query = uuid.toString))
  }
}
