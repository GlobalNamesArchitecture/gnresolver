package org.globalnames
package resolver

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.globalnames.resolver.model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

class Resolver(db: Database, matcher: Matcher) {
  import Resolver.Kind._
  import Resolver.Match

  val gen             = UuidGenerator()
  val nameStrings     = TableQuery[NameStrings]
  val authorWords     = TableQuery[AuthorWords]
  val uninomialWords  = TableQuery[UninomialWords]
  val genusWords      = TableQuery[GenusWords]
  val speciesWords    = TableQuery[SpeciesWords]
  val subspeciesWords = TableQuery[SubspeciesWords]
  val yearWords       = TableQuery[YearWords]

  def resolve(name: String,
              take: Int = 0, drop: Int = 0): Future[Seq[Match]] = {
    val nameUuid = gen.generate(name)
    val exactMatches = nameStrings.filter { ns =>
      ns.id === nameUuid || ns.canonicalUuid === nameUuid
    }

    db.run(exactMatches.result)
      .flatMap { ns =>
        if (ns.isEmpty) {
          def handle(canonicalNameParts: Seq[String]): Future[Seq[Match]] = {
            if (canonicalNameParts.isEmpty) {
              Future.successful(Seq())
            } else {
              val canonicalName = canonicalNameParts.mkString(" ")
              val canonicalNameUuid = gen.generate(canonicalName)
              val exactNameString1 = nameStrings.filter { ns1 =>
                ns1.id === canonicalNameUuid ||
                  ns1.canonicalUuid === canonicalNameUuid
              }
              db.run(exactNameString1.result).flatMap { ns2 =>
                if (ns2.isEmpty) {
                  val fuzzyMatchMap =
                    matcher.transduce(canonicalName)
                      .map { cand =>
                        gen.generate(cand.term) -> cand
                      }.toMap
                  if (fuzzyMatchMap.isEmpty) {
                    handle(canonicalNameParts.dropRight(1))
                  } else {
                    val query2 =
                      nameStrings.filter { ns => ns.canonicalUuid.inSetBind(fuzzyMatchMap.keys) }
                    val fuzzyMatch =
                      db.run(query2.result)
                      .map { ns => ns.map { n => Match(n, Fuzzy(fuzzyMatchMap(n.canonicalName.id).distance)) } }
                    fuzzyMatch
                  }
                } else Future.successful(ns2.map { n => Match(n) })
              }
            }
          }
          val canonicalName =
            snp.fromString(name).canonized(showRanks = false).orZero
          handle(canonicalName.split(' '))
        } else Future.successful(ns.map { n => Match(n) })
      }
  }

  def resolveCanonical(canonicalName: String,
                       take: Int, drop: Int): Future[Seq[Match]] = {
    val canonicalNameUuid = gen.generate(canonicalName)
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonicalUuid === canonicalNameUuid
    }.drop(drop).take(take)
    db.run(queryByCanonicalName.result)
      .map { ns => ns.map { n => Match(n, CanonicalName) } }
  }

  def resolveCanonicalLike(canonicalName: String,
                           take: Int, drop: Int): Future[Seq[Match]] = {
    val queryByCanonicalName = nameStrings.filter { x =>
      x.canonical.like(canonicalName)
    }.drop(drop).take(take)
    db.run(queryByCanonicalName.result)
      .map { ns => ns.map { n => Match(n, CanonicalName) }}
  }

  def resolveAuthor(authorName: String,
                    take: Int, drop: Int): Future[Seq[Match]] = {
    val query = authorWords.filter { x => x.authorWord === authorName }
                           .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
                            .drop(drop).take(take)
    db.run(query2.result)
      .map { ns => ns.map { n => Match(n, Author) }}
  }

  def resolveYear(year: String,
                  take: Int, drop: Int): Future[Seq[Match]] = {
    val query = yearWords.filter { x => x.yearWord === year }
                         .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
                            .drop(drop).take(take)
    db.run(query2.result)
      .map {ns => ns.map { n => Match(n) }}
  }

  def resolveUninomial(uninomial: String,
                       take: Int, drop: Int): Future[Seq[Match]] = {
    val query = uninomialWords.filter { x => x.uninomialWord === uninomial }
                              .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
                            .drop(drop).take(take)
    db.run(query2.result)
      .map { ns => ns.map { n => Match(n) } }
  }

  def resolveGenus(genus: String,
                   take: Int, drop: Int): Future[Seq[Match]] = {
    val query = genusWords.filter { x => x.genusWord === genus }
                          .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
      .drop(drop).take(take)
    db.run(query2.result)
      .map { ns => ns.map { n => Match(n) } }
  }

  def resolveSpecies(species: String,
                     take: Int, drop: Int): Future[Seq[Match]] = {
    val query = speciesWords.filter { x => x.speciesWord === species }
                            .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
                            .drop(drop).take(take)
    db.run(query2.result)
      .map { ns => ns.map { n => Match(n) } }
  }

  def resolveSubspecies(subspecies: String,
                        take: Int, drop: Int): Future[Seq[Match]] = {
    val query = subspeciesWords.filter { x => x.subspeciesWord === subspecies }
                               .map { _.nameStringUuid }
    val query2 = nameStrings.filter { ns => ns.id.in(query) }
                            .drop(drop).take(take)
    db.run(query2.result)
      .map { ns => ns.map { n => Match(n) } }
  }
}

object Resolver {
  sealed trait Kind
  object Kind {
    case object Stub extends Kind

    case object Name extends Kind

    case object CanonicalName extends Kind

    case object Author extends Kind

    case class Fuzzy(score: Int) extends Kind
  }

  case class Match(nameString: NameString, kind: Kind = Kind.Stub)
}
