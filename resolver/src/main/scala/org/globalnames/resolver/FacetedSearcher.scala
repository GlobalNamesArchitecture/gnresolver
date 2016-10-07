package org.globalnames
package resolver

import java.util.UUID

import resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FacetedSearcher(db: Database) {
  private val gen = UuidGenerator()
  private val nameStrings = TableQuery[NameStrings]
  private val authorWords = TableQuery[AuthorWords]
  private val uninomialWords = TableQuery[UninomialWords]
  private val genusWords = TableQuery[GenusWords]
  private val speciesWords = TableQuery[SpeciesWords]
  private val subspeciesWords = TableQuery[SubspeciesWords]
  private val yearWords = TableQuery[YearWords]
  private val dataSources = TableQuery[DataSources]
  private val nameStringIndicies = TableQuery[NameStringIndices]

  def resolveCanonical(canonicalName: String, take: Int, drop: Int): Future[Matches] = {
    val canonicalNameUuid = gen.generate(canonicalName)
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonicalUuid === canonicalNameUuid
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, canonicalName)
  }

  def resolveCanonicalLike(canonicalName: String, take: Int, drop: Int): Future[Matches] = {
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonical.like(canonicalName)
    }
    val queryByCanonicalNamePortion = queryByCanonicalName.drop(drop).take(take)
    val queryByCanonicalNameCount = queryByCanonicalName.countDistinct
    for {
      portion <- db.run(queryByCanonicalNamePortion.result)
      count <- db.run(queryByCanonicalNameCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, canonicalName)
  }

  def resolveAuthor(authorName: String, take: Int, drop: Int): Future[Matches] = {
    val query = authorWords.filter { x => x.authorWord === authorName }
                           .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, authorName)
  }

  def resolveYear(year: String, take: Int, drop: Int): Future[Matches] = {
    val query = yearWords.filter { x => x.yearWord === year }
                         .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, year)
  }

  def resolveUninomial(uninomial: String, take: Int, drop: Int): Future[Matches] = {
    val query = uninomialWords.filter { x => x.uninomialWord === uninomial }
                              .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, uninomial)
  }

  def resolveGenus(genus: String, take: Int, drop: Int): Future[Matches] = {
    val query = genusWords.filter { x => x.genusWord === genus }
                          .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, genus)
  }

  def resolveSpecies(species: String, take: Int, drop: Int): Future[Matches] = {
    val query = speciesWords.filter { x => x.speciesWord === species }
                            .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, species)
  }

  def resolveSubspecies(subspecies: String, take: Int, drop: Int): Future[Matches] = {
    val query = subspeciesWords.filter { x => x.subspeciesWord === subspecies }
                               .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
    val query2count = query2.countDistinct
    val query2portion = query2.drop(drop).take(take)
    for {
      portion <- db.run(query2portion.result)
      count <- db.run(query2count.result)
    } yield Matches(count, portion.map { n => Match(n) }, subspecies)
  }

  def resolveNameStrings(nameStringQuery: String, take: Int, drop: Int): Future[Matches] = {
    val query = nameStrings.filter { ns => ns.name === nameStringQuery }
    val queryCount = query.countDistinct
    val queryPortion = query.drop(drop).take(take)
    for {
      portion <- db.run(queryPortion.result)
      count <- db.run(queryCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, nameStringQuery)
  }

  def resolveNameStringsLike(nameStringQuery: String, take: Int, drop: Int): Future[Matches] = {
    val query = nameStrings.filter { ns => ns.name.like(nameStringQuery) }
    val queryCount = query.countDistinct
    val queryPortion = query.drop(drop).take(take)
    for {
      portion <- db.run(queryPortion.result)
      count <- db.run(queryCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, nameStringQuery)
  }

  def resolveExact(exact: String, take: Int, drop: Int): Future[Matches] = {
    val query = nameStrings.filter { ns => ns.canonical === exact || ns.name === exact }
    val queryCount = query.countDistinct
    val queryPortion = query.drop(drop).take(take)
    for {
      portion <- db.run(queryPortion.result)
      count <- db.run(queryCount.result)
    } yield Matches(count, portion.map { n => Match(n) }, exact)
  }

  def resolveDataSources(uuid: UUID): Future[Seq[(NameStringIndex, DataSource)]] = {
    val query = for {
      nsi <- nameStringIndicies
      ds <- dataSources
      if nsi.nameStringId === uuid && nsi.dataSourceId === ds.id
    } yield (nsi, ds)

    db.run(query.result)
  }

  def findNameStringByUuid(uuid: UUID): Future[Matches] = {
    val query = nameStrings.filter { ns => ns.id === uuid }.take(1)
    db.run(query.result).map { xs => Matches(xs.size, xs.map { x => Match(x) }, uuid.toString) }
  }
}
