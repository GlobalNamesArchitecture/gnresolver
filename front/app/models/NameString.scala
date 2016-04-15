package models

import java.util.UUID

case class Name(id: UUID, value: String)
case class NameString(name: Name, canonicalName: Name)
case class Match(nameString: NameString)
case class Response(total: Int, matches: Seq[Match])
