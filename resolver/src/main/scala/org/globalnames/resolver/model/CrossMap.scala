package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class CrossMap(dataSourceId: Int, nameStringId: UUID, taxonId: String,
                    dataSourceIdCrossMap: Int, localId: String)

class CrossMaps(tag: Tag) extends Table[CrossMap](tag, "cross_maps") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def nameStringId: Rep[UUID] = column[UUID]("name_string_id")

  def taxonId: Rep[String] = column[String]("taxon_id")

  def dataSourceIdCrossMap: Rep[Int] = column[Int]("cm_data_source_id")

  def localId: Rep[String] = column[String]("cm_local_id")

  def * : ProvenShape[CrossMap] =
    (dataSourceId, nameStringId, taxonId, dataSourceIdCrossMap, localId) <>
      (CrossMap.tupled, CrossMap.unapply)
}
