package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape

case class AuthorWord(word: String, nameString: UUID)

class AuthorWords(tag: Tag)
  extends Table[AuthorWord](tag, "name_strings__author_words") {

  def authorWord: Rep[String] = column[String]("author_word")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[AuthorWord] = (authorWord, nameStringUuid) <>
    (AuthorWord.tupled, AuthorWord.unapply)
}
