package org.globalnames.resolver.model.db

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class YearWord(word: String, nameString: UUID)

class YearWords(tag: Tag)
  extends Table[YearWord](tag, "name_strings__year") {

  def yearWord: Rep[String] = column[String]("year")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[YearWord] = (yearWord, nameStringUuid) <> (YearWord.tupled, YearWord.unapply)
}
