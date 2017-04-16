package org.globalnames.resolver.model.db

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class SubspeciesWord(word: String, nameString: UUID)

class SubspeciesWords(tag: Tag)
  extends Table[SubspeciesWord](tag, "name_strings__subspecies") {

  def subspeciesWord: Rep[String] = column[String]("subspecies")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[SubspeciesWord] = (subspeciesWord, nameStringUuid) <>
    (SubspeciesWord.tupled, SubspeciesWord.unapply)
}
