package org.globalnames
package resolver

import scalaz._
import Scalaz._

package object model {

  sealed trait MatchType
  object MatchType {
    case object None extends MatchType
    case object ExactNameMatchByUUID extends MatchType
    case object ExactCanonicalNameMatchByUUID extends MatchType
    case object Fuzzy extends MatchType
    case object UUIDLookup extends MatchType
  }

  type LocalId = Int

  case class Match(nameString: NameString, dataSource: DataSource, nameStringIndex: NameStringIndex,
                   vernacularStrings: Seq[(VernacularString, VernacularStringIndex)],
                   matchType: MatchType = MatchType.None)

  case class Matches(total: Long, matches: Seq[Match],
                     suppliedNameString: Option[String] = None, localId: Option[LocalId] = None)

  object Matches {
    def empty(suppliedNameString: String): Matches = {
      Matches(0, Seq(), suppliedNameString.some)
    }
  }

}
