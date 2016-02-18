package org.globalnames
package resolver

import java.io.Closeable
import java.util.UUID

import slick.driver.MySQLDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class PostgreSqlDAL(timeout: Duration, step: Int)
  extends Closeable with UUIDPlainImplicits {

  val postgresqlDb = Database.forConfig("postgresql")

  def version: String = {
    val query = sql"""select version();""".as[String].head
    Await.result(postgresqlDb.run(query), timeout)
  }

  def writeNameString(nameUuid: UUID, nameUuidOriginal: UUID, name: String,
                      canonicalUuid: UUID, canonical: String): Unit = {
    val query = sqlu"""
      INSERT INTO name_strings (id, id_mysql, name, canonical_uuid, canonical)
      VALUES ($nameUuid, $nameUuidOriginal, $name, $canonicalUuid, $canonical);"""
    Await.result(postgresqlDb.run(query), timeout)
  }

  def insertNameStringIndicies(nsid: NameStringIndexData): Unit = {
    val query = sqlu"""
              INSERT INTO name_string_indices (
                data_source_id, name_string_id, taxon_id, rank,
                accepted_taxon_id, classification_path,
                classification_path_ids,
                classification_path_ranks)
              VALUES (${nsid.dataSourceId}, ${nsid.nameStringId},
                      ${nsid.taxonId}, ${nsid.rank}, ${nsid.acceptedTaxonId},
                      ${nsid.classificationPath}, ${nsid.classificationPathIds},
                      ${nsid.classificationPathRanks});"""
    Await.result(postgresqlDb.run(query), timeout)
  }

  override def close(): Unit = postgresqlDb.close()
}
