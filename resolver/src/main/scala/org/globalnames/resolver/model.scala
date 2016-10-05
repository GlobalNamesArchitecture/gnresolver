package org.globalnames
package resolver

package object model {

  sealed trait Kind
  object Kind {
    case object None extends Kind
    case class Fuzzy(score: Int) extends Kind
  }

  type LocalId = Int

  case class Match(nameString: NameString, dataSourceId: Int = 0, kind: Kind = Kind.None)

  case class Matches(total: Long, matches: Seq[Match],
                     suppliedNameString: String, localId: Option[LocalId] = None)

}
