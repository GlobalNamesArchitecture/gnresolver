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

    val queryByNameId = nameStrings.filter { x =>
      x.id === UUID.fromString(parsed.input.id)
    }
    val queryByCanonicalNameId = nameStrings.filter { x =>
      x.canonicalUuid ===
        gen.generate(parsed.canonized(showRanks = false).toString)
    }
    val fuzzyMatches = matcher.transduce(name).map { _.term }
    for {
      namesMatch <- db.run(queryByNameId.result)
      canonicalNamesMatch <- db.run(queryByCanonicalNameId.result)
    } yield {
      Match(namesMatch, canonicalNamesMatch, fuzzyMatches)
    }
  }
}

object Resolver {
  case class Match(byName: Seq[NameString], byCanonicalName: Seq[NameString],
                   byCanonicalNameFuzzy: Seq[String])
}
