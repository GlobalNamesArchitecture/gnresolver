package org.globalnames.resolver.model.db

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class NameStringIndex(dataSourceId: Int, nameStringId: UUID, url: Option[String],
                           taxonId: String, globalId: Option[String], localId: Option[String],
                           nomenclaturalCodeId: Option[Int], rank: Option[String],
                           acceptedTaxonId: Option[String], classificationPath: Option[String],
                           classificationPathIds: Option[String],
                           classificationPathRanks: Option[String])

class NameStringIndices(tag: Tag)
  extends Table[NameStringIndex](tag, "name_string_indices") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def nameStringId: Rep[UUID] = column[UUID]("name_string_id")

  def url: Rep[Option[String]] = column[Option[String]]("url")

  def taxonId: Rep[String] = column[String]("taxon_id")

  def globalId: Rep[Option[String]] = column[Option[String]]("global_id")

  def localId: Rep[Option[String]] = column[Option[String]]("local_id")

  def nomenclaturalCodeId: Rep[Option[Int]] = column[Option[Int]]("nomenclatural_code_id")

  def rank: Rep[Option[String]] = column[Option[String]]("rank")

  def acceptedTaxonId: Rep[Option[String]] = column[Option[String]]("accepted_taxon_id")

  def classificationPath: Rep[Option[String]] = column[Option[String]]("classification_path")

  def classificationPathIds: Rep[Option[String]] = column[Option[String]]("classification_path_ids")

  def classificationPathRanks: Rep[Option[String]] =
    column[Option[String]]("classification_path_ranks")

  def * : ProvenShape[NameStringIndex] =
    (dataSourceId, nameStringId, url, taxonId, globalId, localId, nomenclaturalCodeId, rank,
      acceptedTaxonId, classificationPath, classificationPathIds, classificationPathRanks) <>
    (NameStringIndex.tupled, NameStringIndex.unapply)
}
