package org.globalnames.resolver.model.db

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class VernacularStringIndex(dataSourceId: Int, taxonId: String, vernacularStringId: UUID,
                                 language: Option[String], locality: Option[String],
                                 countryCode: Option[String])

class VernacularStringIndices(tag: Tag)
  extends Table[VernacularStringIndex](tag, "vernacular_string_indices") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def taxonId: Rep[String] = column[String]("taxon_id")

  def vernacularStringId: Rep[UUID] = column[UUID]("vernacular_string_id")

  def language: Rep[Option[String]] = column[Option[String]]("language")

  def locality: Rep[Option[String]] = column[Option[String]]("locality")

  def countryCode: Rep[Option[String]] = column[Option[String]]("country_code")

  def * : ProvenShape[VernacularStringIndex] =
    (dataSourceId, taxonId, vernacularStringId, language, locality, countryCode) <>
      (VernacularStringIndex.tupled, VernacularStringIndex.unapply)
}
