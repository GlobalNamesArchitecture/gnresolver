package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class CrossMap(dataSourceId: Int, nameStringId: UUID, localId: String)

class CrossMaps(tag: Tag) extends Table[CrossMap](tag, "cross_maps") {

  def dataSourceId   = column[Int]("data_source_id")

  def nameStringId   = column[UUID]("name_string_id")

  def localId        = column[String]("local_id")

  def * = (dataSourceId, nameStringId, localId) <> (CrossMap.tupled, CrossMap.unapply)
}
