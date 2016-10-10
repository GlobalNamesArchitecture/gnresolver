package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class UninomialWord(word: String, nameString: UUID)

class UninomialWords(tag: Tag)
  extends Table[UninomialWord](tag, "name_strings__uninomial") {

  def uninomialWord: Rep[String] = column[String]("uninomial")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[UninomialWord] = (uninomialWord, nameStringUuid) <>
    (UninomialWord.tupled, UninomialWord.unapply)
}
