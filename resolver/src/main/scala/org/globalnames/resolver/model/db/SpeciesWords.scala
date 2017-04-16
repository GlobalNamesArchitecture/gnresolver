package org.globalnames.resolver.model.db

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class SpeciesWord(word: String, nameString: UUID)

class SpeciesWords(tag: Tag)
  extends Table[SpeciesWord](tag, "name_strings__species") {

  def speciesWord: Rep[String] = column[String]("species")

  def nameStringUuid: Rep[UUID] = column[UUID]("name_uuid")

  def * : ProvenShape[SpeciesWord] = (speciesWord, nameStringUuid) <>
    (SpeciesWord.tupled, SpeciesWord.unapply)
}
