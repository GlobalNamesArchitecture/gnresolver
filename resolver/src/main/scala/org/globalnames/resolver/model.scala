package org.globalnames
package resolver

import scalaz._
import Scalaz._

package object model {

  sealed trait MatchType
  object MatchType {
    case object Unknown extends MatchType
    case object ExactNameMatchByUUID extends MatchType
    case object ExactNameMatchByString extends MatchType
    case object ExactCanonicalNameMatchByUUID extends MatchType
    case object ExactCanonicalNameMatchByString extends MatchType
    case object Fuzzy extends MatchType
    case object ExactMatchPartialByGenus extends MatchType
    case object UUIDLookup extends MatchType
  }

  type SuppliedId = Int

  case class Match(nameString: NameString, dataSource: DataSource, nameStringIndex: NameStringIndex,
                   vernacularStrings: Seq[(VernacularString, VernacularStringIndex)],
                   nameType: Option[Int], matchType: MatchType = MatchType.Unknown)

  case class Matches(total: Long, matches: Seq[Match],
                     suppliedNameString: Option[String] = None,
                     suppliedId: Option[SuppliedId] = None)

  object Matches {
    def empty: Matches = Matches(0, Seq(), None)

    def empty(suppliedNameString: String): Matches = {
      Matches(0, Seq(), suppliedNameString.some)
    }
  }

}
