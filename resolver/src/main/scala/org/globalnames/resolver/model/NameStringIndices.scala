package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class NameStringIndex(dataSourceId: Int, nameStringId: UUID, url: Option[String])

class NameStringIndices(tag: Tag)
  extends Table[NameStringIndex](tag, "name_string_indices") {

  def dataSourceId   = column[Int]("data_source_id")

  def nameStringId   = column[UUID]("name_string_id")

  def url            = column[Option[String]]("url")

  def * = (dataSourceId, nameStringId, url) <>
    (NameStringIndex.tupled, NameStringIndex.unapply)
}
