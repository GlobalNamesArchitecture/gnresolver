package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class UninomialWord(word: String, nameString: UUID)

class UninomialWords(tag: Tag)
  extends Table[UninomialWord](tag, "name_strings_uninomial") {

  def uninomialWord  = column[String]("uninomial")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (uninomialWord, nameStringUuid) <>
    (UninomialWord.tupled, UninomialWord.unapply)
}
