package org.globalnames.resolver.model

import java.util.UUID

import slick.driver.PostgresDriver.api._

case class SpeciesWord(word: String, nameString: UUID)

class SpeciesWords(tag: Tag)
  extends Table[SpeciesWord](tag, "name_strings_species") {

  def speciesWord = column[String]("species")

  def nameStringUuid = column[UUID]("name_uuid")

  def * = (speciesWord, nameStringUuid) <>
    (SpeciesWord.tupled, SpeciesWord.unapply)
}
