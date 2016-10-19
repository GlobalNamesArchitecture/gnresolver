package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class Name(id: UUID, value: String)
case class NameString(name: Name, canonicalName: Option[Name])

class NameStrings(tag: Tag) extends Table[NameString](tag, "name_strings") {
  import NameStrings.emptyCanonicalUuid

  def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

  def name: Rep[String] = column[String]("name")

  def canonicalUuid: Rep[Option[UUID]] = column[Option[UUID]]("canonical_uuid")

  def canonical: Rep[Option[String]] = column[Option[String]]("canonical")

  def * : ProvenShape[NameString] = ((id, name), (canonicalUuid, canonical)).shaped <> (
      { case (name, (canonicalNameUuid, canonicalNameValue)) =>
        val canonicalName =
          for {
            uuid <- canonicalNameUuid
            if uuid != emptyCanonicalUuid
            value <- canonicalNameValue
          } yield Name(uuid, value)
        NameString(Name.tupled.apply(name), canonicalName)
      },
      { ns: NameString =>
        Some(((ns.name.id, ns.name.value),
              (ns.canonicalName.map { _.id }, ns.canonicalName.map { _.value })))
      }
    )
}

object NameStrings {
  private[NameStrings] val emptyCanonicalUuid =
    UUID.fromString("a9456e61-bd30-53bc-8588-accb913cc64a")
}