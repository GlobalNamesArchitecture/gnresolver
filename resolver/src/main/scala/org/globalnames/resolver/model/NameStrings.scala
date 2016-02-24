package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class Name(id: UUID, value: String)
case class NameString(name: Name, canonicalName: Name)

class NameStrings(tag: Tag) extends Table[NameString](tag, "name_strings") {
  def id            = column[UUID]("id", O.PrimaryKey)

  def name          = column[String]("name")

  def canonicalUuid = column[UUID]("canonical_uuid")

  def canonical     = column[String]("canonical")

  def * = ((id, name), (canonicalUuid, canonical)).shaped <> (
      { case (name, canonicalName) =>
        NameString(Name.tupled.apply(name), Name.tupled.apply(canonicalName)) },
      { ns: NameString =>
        Some(((ns.name.id, ns.name.value),
              (ns.canonicalName.id, ns.canonicalName.value)))
      }
    )
}
