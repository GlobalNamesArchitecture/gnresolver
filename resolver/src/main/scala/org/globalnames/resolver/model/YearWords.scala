package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class YearWord(word: String, nameString: UUID)

class YearWords(tag: Tag)
  extends Table[YearWord](tag, "name_strings_year") {

  def yearWord       = column[String]("year")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (yearWord, nameStringUuid) <>
    (YearWord.tupled, YearWord.unapply)
}
