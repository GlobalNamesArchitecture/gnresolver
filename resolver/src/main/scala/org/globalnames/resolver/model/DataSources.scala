package org.globalnames.resolver.model

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class DataSource(id: Int, title: String, description: String)

class DataSources(tag: Tag)
  extends Table[DataSource](tag, "data_sources") {

  def id: Rep[Int] = column[Int]("id", O.PrimaryKey)

  def title: Rep[String] = column[String]("title")

  def description: Rep[String] = column[String]("description")

  def * : ProvenShape[DataSource] = (id, title, description) <>
    (DataSource.tupled, DataSource.unapply)
}
