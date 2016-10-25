package org.globalnames
package resolver

import model._
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CrossMap(db: Database) {
  private val crossMaps = TableQuery[CrossMaps]

  import CrossMap._

  def execute(databaseSourceId: Int, databaseSinkIds: Seq[Int], databaseTargetId: Option[Int],
              localIds: Seq[String]): Future[Seq[Result]] = {
    if (databaseSourceId < 0 || databaseSinkIds.exists { _ < 0 } ||
        databaseTargetId.exists { _ < 0 }) {
      Future.successful(localIds.map { lid => Result(Source(databaseSourceId, lid), Seq()) })
    } else {
      val localIdsSet = Set(localIds: _*)
      val query = for {
        mapping <- crossMaps.filter { cm =>
          cm.dataSourceIdCrossMap === databaseSourceId && cm.localId.inSetBind(localIdsSet)
        }
        target <- crossMaps.filter { cm =>
          cm.dataSourceId === mapping.dataSourceId &&
            cm.taxonId === mapping.taxonId &&
            cm.nameStringId === mapping.nameStringId &&
            (databaseSinkIds.isEmpty.bind || cm.dataSourceId.inSetBind(databaseSinkIds)) &&
            (databaseTargetId.isEmpty.bind || cm.dataSourceIdCrossMap === databaseTargetId) &&
            cm.dataSourceIdCrossMap =!= databaseSourceId
        }
      } yield (mapping.localId, target)

      db.run(query.result).map { mapped =>
        val m = mapped.foldLeft(Map.empty[String, Seq[model.CrossMap]]) { case (acc, (k, v)) =>
          acc.updated(k, v +: acc.getOrElse(k, Seq()))
        }
        localIds.map { lid =>
          Result(Source(databaseSourceId, lid), m.getOrElse(lid, Seq()).map { t =>
            Target(t.dataSourceId, t.dataSourceIdCrossMap, t.localId)
          })
        }
      }
    }
  }
}

object CrossMap {
  case class Source(dbId: Int, localId: String)
  case class Target(dbSinkId: Int, dbTargetId: Int, localId: String) {
    override def toString: String = s"($dbSinkId)~>($dbTargetId,$localId)"
  }
  case class Result(source: Source, target: Seq[Target]) {
    override def toString: String =
      s"CrossMapResult(${source.dbId},${source.localId})~${target.mkString("[", "|", "]")})"
  }
}
