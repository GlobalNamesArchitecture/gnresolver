package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class GenusWord(word: String, nameString: UUID)

class GenusWords(tag: Tag)
  extends Table[GenusWord](tag, "name_strings_genus") {

  def genusWord      = column[String]("genus")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (genusWord, nameStringUuid) <>
    (GenusWord.tupled, GenusWord.unapply)
}
