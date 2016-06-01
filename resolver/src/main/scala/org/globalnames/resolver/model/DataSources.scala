package org.globalnames.resolver.model

import slick.driver.PostgresDriver.api._

case class DataSource(id: Int, title: String, description: String)

class DataSources(tag: Tag)
  extends Table[DataSource](tag, "data_sources") {

  def id             = column[Int]("id", O.PrimaryKey)

  def title          = column[String]("title")

  def description    = column[String]("description")

  def * = (id, title, description) <>
    (DataSource.tupled, DataSource.unapply)
}
