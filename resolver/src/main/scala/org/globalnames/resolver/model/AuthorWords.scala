package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class AuthorWord(word: String, nameString: UUID)

class AuthorWords(tag: Tag)
  extends Table[AuthorWord](tag, "name_strings_author_words") {

  def authorWord     = column[String]("author_word")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (authorWord, nameStringUuid) <>
    (AuthorWord.tupled, AuthorWord.unapply)
}
