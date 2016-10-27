package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class VernacularStringIndex(dataSourceId: Int, taxonId: String, vernacularStringId: UUID,
                                 language: String, locality: String, countryCode: String)

class VernacularStringIndices(tag: Tag)
  extends Table[VernacularStringIndex](tag, "vernacular_string_indices") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def taxonId: Rep[String] = column[String]("taxon_id")

  def vernacularStringId: Rep[UUID] = column[UUID]("vernacular_string_id")

  def language: Rep[String] = column[String]("language")

  def locality: Rep[String] = column[String]("locality")

  def countryCode: Rep[String] = column[String]("country_code")

  def * : ProvenShape[VernacularStringIndex] =
    (dataSourceId, taxonId, vernacularStringId, language, locality, countryCode) <>
      (VernacularStringIndex.tupled, VernacularStringIndex.unapply)
}
