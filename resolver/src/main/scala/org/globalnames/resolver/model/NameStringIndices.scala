package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class NameStringIndex(dataSourceId: Int, nameStringId: UUID, url: Option[String])

class NameStringIndices(tag: Tag)
  extends Table[NameStringIndex](tag, "name_string_indices") {

  def dataSourceId: Rep[Int] = column[Int]("data_source_id")

  def nameStringId: Rep[UUID] = column[UUID]("name_string_id")

  def url: Rep[Option[String]] = column[Option[String]]("url")

  def * : ProvenShape[NameStringIndex] = (dataSourceId, nameStringId, url) <>
    (NameStringIndex.tupled, NameStringIndex.unapply)
}
