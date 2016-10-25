package org.globalnames
package resolver

import model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CrossMap(db: Database) {
  private val nameStringIndicies = TableQuery[NameStringIndices]
  private val crossMaps = TableQuery[CrossMaps]

  def execute(databaseSourceId: Int, databaseSinkIds: Seq[Int], databaseTargetId: Int,
              taxonIds: Seq[String], localIds: Seq[String]): Future[Seq[(String, String)]] = {
    if (databaseSourceId < 0 || databaseSinkIds.exists { _ < 0 } || databaseTargetId < 0) {
      Future.successful(taxonIds.map { (_, "") })
    } else {
      // Slick doesn't give facilities to create temporary table from `localIds` and
      // make join against it. Until https://github.com/slick/slick/issues/799 is solved
      val existingCrossMapsQuery = crossMaps.filter { cm =>
        cm.dataSourceId === databaseSourceId &&
          (databaseSinkIds.isEmpty.bind || cm.dataSourceIdCrossMap.inSetBind(databaseSinkIds)) &&
          cm.localId.inSetBind(localIds) &&
          cm.taxonId.inSetBind(taxonIds)
      }

      val query = for {
        sourceCrossMap <- existingCrossMapsQuery
        mapping <- nameStringIndicies.filter { nsi =>
          nsi.nameStringId === sourceCrossMap.nameStringId
        }
        targetCrossMap <- crossMaps.filter { cm =>
          cm.dataSourceId === databaseTargetId &&
            (databaseSinkIds.isEmpty.bind || cm.dataSourceIdCrossMap.inSetBind(databaseSinkIds)) &&
            cm.nameStringId === mapping.nameStringId &&
            cm.localId.inSetBind(localIds)
        }
      } yield (sourceCrossMap.taxonId, targetCrossMap.taxonId)

      for {
        mapped <- db.run(query.result).map { _.distinct }
        unmatchedTaxonIds = taxonIds diff mapped.map { case (taxId, _) => taxId }
      } yield mapped ++ unmatchedTaxonIds.map { (_, "") }
    }
  }
}
