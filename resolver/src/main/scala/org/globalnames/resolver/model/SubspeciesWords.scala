package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class SubspeciesWord(word: String, nameString: UUID)

class SubspeciesWords(tag: Tag)
  extends Table[SubspeciesWord](tag, "name_strings_subspecies") {

  def subspeciesWord = column[String]("subspecies")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (subspeciesWord, nameStringUuid) <>
    (SubspeciesWord.tupled, SubspeciesWord.unapply)
}
