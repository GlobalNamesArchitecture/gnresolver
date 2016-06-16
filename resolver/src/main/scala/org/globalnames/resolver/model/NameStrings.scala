package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class Name(id: UUID, value: String)
case class NameString(name: Name, canonicalName: Option[Name])

class NameStrings(tag: Tag) extends Table[NameString](tag, "name_strings") {
  def id            = column[UUID]("id", O.PrimaryKey)

  def name          = column[String]("name")

  def canonicalUuid = column[Option[UUID]]("canonical_uuid")

  def canonical     = column[Option[String]]("canonical")

  def * = ((id, name), (canonicalUuid, canonical)).shaped <> (
      { case (name, (canonicalNameUuid, canonicalNameValue)) =>
        val canonicalName =
          for (uuid <- canonicalNameUuid; value <- canonicalNameValue)
            yield Name(uuid, value)
        NameString(Name.tupled.apply(name), canonicalName)
      },
      { ns: NameString =>
        Some(((ns.name.id, ns.name.value),
              (ns.canonicalName.map { _.id }, ns.canonicalName.map { _.value })))
      }
    )
}
