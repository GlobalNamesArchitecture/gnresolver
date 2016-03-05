package org.globalnames
package resolver

import java.util.UUID

import parser.ScientificNameParser.{instance => snp}
import model.{NameString, NameStrings}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Resolver(db: Database, matcher: Matcher) {
  import Resolver.Match

  val nameStrings = TableQuery[NameStrings]

  def resolve(name: String): Future[Resolver.Match] = {
    val gen = UuidGenerator()

    val cleanedName = name.replaceAll("\\s+", " ")
    val parsed = snp.fromString(cleanedName)

    val names = db.run(nameStrings.filter { x =>
      x.id === UUID.fromString(parsed.input.id)
    }.result)
    val canonicalNames = parsed.canonized(showRanks = false).map { canonical =>
      val queryByCanonicalNameId = nameStrings.filter { x =>
        x.canonicalUuid === gen.generate(canonical)
      }
      db.run(queryByCanonicalNameId.result)
    }.getOrElse(Future.successful(Seq()))
    val fuzzyMatches = matcher.transduce(name).map { _.term }
    for {
      namesMatch <- names
      canonicalNamesMatch <- canonicalNames
    } yield {
      Match(namesMatch, canonicalNamesMatch, fuzzyMatches)
    }
  }
}

object Resolver {
  case class Match(byName: Seq[NameString], byCanonicalName: Seq[NameString],
                   byCanonicalNameFuzzy: Seq[String])
}
