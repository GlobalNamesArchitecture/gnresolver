package org.globalnames.resolver.model.db

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class VernacularString(id: UUID, name: String)

class VernacularStrings(tag: Tag) extends Table[VernacularString](tag, "vernacular_strings") {
  def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

  def name: Rep[String] = column[String]("name")

  def * : ProvenShape[VernacularString] =
    (id, name) <> (VernacularString.tupled, VernacularString.unapply)
}
