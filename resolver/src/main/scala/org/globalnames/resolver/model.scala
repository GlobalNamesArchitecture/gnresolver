package org.globalnames
package resolver

package object model {

  sealed trait Kind
  object Kind {
    case object None extends Kind
    case object ExactNameMatchByUUID extends Kind
    case object ExactCanonicalNameMatchByUUID extends Kind
    case object Fuzzy extends Kind
  }

  type LocalId = Int

  case class Match(nameString: NameString, dataSource: DataSource, nameStringIndex: NameStringIndex,
                   vernacularStrings: Seq[(VernacularString, VernacularStringIndex)],
                   kind: Kind = Kind.None)

  case class Matches(total: Long, matches: Seq[Match],
                     suppliedNameString: String = "", localId: Option[LocalId] = None)
  object Matches {
    def empty(suppliedNameString: String): Matches = {
      Matches(0, Seq(), suppliedNameString)
    }
  }

}
