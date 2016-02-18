package org.globalnames
package resolver

import java.util.UUID

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.globalnames.resolver.model.NameStrings
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Resolver(db: Database) {
  val nameStrings = TableQuery[NameStrings]

  def resolve(name: String): Future[Seq[(UUID, String, UUID, String)]] = {
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
    db.run(queryByNameId.result).flatMap {
      case Seq() => db.run(queryByCanonicalNameId.result)
      case names => Future.successful(names)
    }
  }
}
