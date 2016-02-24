package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class NameString(id: UUID, name: String, canonicalUuid: UUID, canonical: String)

class NameStrings(tag: Tag) extends Table[(UUID, String, UUID, String)](tag, "name_strings") {
  def id            = column[UUID]("id", O.PrimaryKey)

  def name          = column[String]("name")

  def canonicalUuid = column[UUID]("canonical_uuid")

  def canonical     = column[String]("canonical")

  def * = (id, name, canonicalUuid, canonical)
}
