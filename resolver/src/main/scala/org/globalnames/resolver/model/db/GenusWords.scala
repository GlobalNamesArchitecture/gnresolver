package org.globalnames.resolver.model.db

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class GenusWord(word: String, nameString: UUID)

class GenusWords(tag: Tag)
  extends Table[GenusWord](tag, "name_strings__genus") {

  def genusWord: Rep[String] = column[String]("genus")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[GenusWord] = (genusWord, nameStringUuid) <>
    (GenusWord.tupled, GenusWord.unapply)
}
