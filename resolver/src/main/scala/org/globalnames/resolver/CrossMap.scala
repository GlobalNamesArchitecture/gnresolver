package org.globalnames
package resolver

import model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CrossMap(db: Database) {
  val nameStringIndicies = TableQuery[NameStringIndices]
  val crossMaps = TableQuery[CrossMaps]

  def execute(databaseSourceId: Int, databaseTargetId: Int,
              localIds: Seq[String]): Future[Seq[(String, String)]] = {
    if (databaseSourceId < 0 || databaseTargetId < 0) {
      Future.successful(localIds.map { (_, "") })
    } else {
      // Slick doesn't give facilities to create temporary table from `localIds` and
      // make join against it. Until https://github.com/slick/slick/issues/799 is solved
      val existingCrossMapsQuery = crossMaps.filter { cm =>
        cm.dataSourceId === databaseSourceId && cm.localId.inSetBind(localIds)
      }
      val existingLocalIds = db.run(existingCrossMapsQuery.map { _.localId }.result)

      val query = for {
        sourceCrossMap <- existingCrossMapsQuery
        mapping <- nameStringIndicies.filter { nsi =>
          nsi.nameStringId === sourceCrossMap.nameStringId
        }
        targetCrossMap <- crossMaps.filter { cm =>
          cm.dataSourceId === databaseTargetId && cm.nameStringId === mapping.nameStringId
        }
      } yield (sourceCrossMap.localId, targetCrossMap.localId)

      for {
        mapped <- db.run(query.result).map { _.distinct }
        existing <- existingLocalIds
      } yield mapped ++ (localIds diff existing).map { (_, "") }
    }
  }
}
