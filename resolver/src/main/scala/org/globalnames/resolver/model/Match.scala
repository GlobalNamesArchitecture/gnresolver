package org.globalnames
package resolver
package model

import scalaz._
import Scalaz._

import db.{NameString, DataSource, NameStringIndex, VernacularString, VernacularStringIndex}
import parser.ScientificNameParser.{Result => SNResult}

case class Match(nameString: NameString, dataSource: DataSource, nameStringIndex: NameStringIndex,
                 vernacularStrings: Seq[(VernacularString, VernacularStringIndex)],
                 nameType: Option[Int], matchType: MatchType = MatchType.Unknown) {
  val synonym: Boolean = {
    val classificationPathIdsSeq =
      nameStringIndex.classificationPathIds.map { cpids => cpids.split('|').toList }
        .getOrElse(List())
    if (classificationPathIdsSeq.nonEmpty) {
      nameStringIndex.taxonId == classificationPathIdsSeq.last
    } else if (nameStringIndex.acceptedTaxonId.isDefined) {
      nameStringIndex.taxonId == nameStringIndex.acceptedTaxonId.get
    } else false
  }
}

case class Matches(total: Long, matches: Seq[Match],
                   suppliedInput: Option[String] = None,
                   private val suppliedIdProvided: Option[SuppliedId] = None,
                   scientificName: Option[SNResult] = None) {
  val suppliedId: Option[SuppliedId] = suppliedIdProvided.map { _.trim }
}

object Matches {
  def empty: Matches = Matches(0, Seq(), None)

  def empty(suppliedInput: String, suppliedId: Option[SuppliedId] = None): Matches =
    Matches(0, Seq(), suppliedInput.some, suppliedIdProvided = suppliedId)
}
