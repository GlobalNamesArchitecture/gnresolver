package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class CrossMap(dataSourceId: Int, nameStringId: UUID, localId: String)

class CrossMaps(tag: Tag) extends Table[CrossMap](tag, "cross_maps") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def nameStringId: Rep[UUID] = column[UUID]("name_string_id")

  def localId: Rep[String] = column[String]("local_id")

  def * : ProvenShape[CrossMap] = (dataSourceId, nameStringId, localId) <>
    (CrossMap.tupled, CrossMap.unapply)
}
