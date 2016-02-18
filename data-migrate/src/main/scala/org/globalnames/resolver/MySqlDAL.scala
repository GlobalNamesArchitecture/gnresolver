package org.globalnames.resolver

import java.io.Closeable

import slick.driver.MySQLDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class MySqlDAL(timeout: Duration, step: Int)
  extends Closeable with UUIDPlainImplicits {

  val mysqlDb = Database.forConfig("mysql")

  def nameStringIndicesCount: Int = {
    val query = sql"SELECT count(*) FROM name_string_indices;".as[Int].head
    Await.result(mysqlDb.run(query), timeout)
  }

  def nameStringsCount: Int = {
    val query = sql"SELECT count(*) FROM name_strings;".as[Int].head
    Await.result(mysqlDb.run(query), timeout)
  }

  def nameStringsData(idx: Int): Vector[(Int, String, String)] = {
    val query =
      sql"""SELECT id, `name`, uuid
                  FROM name_strings
                  LIMIT ${idx * step}, $step;""".as[(Int, String, String)]
    Await.result(mysqlDb.run(query), timeout)
  }

  def nameStringsIndiciesData(idx: Int): Vector[NameStringIndexData] = {
    val query =
      sql"""SELECT data_source_id, name_string_id, taxon_id, rank,
                   accepted_taxon_id, synonym, classification_path,
                   classification_path_ids, created_at, updated_at,
                   classification_path_ranks
            FROM name_string_indices
            LIMIT ${idx * step}, $step;"""
        .as[(Int, Int, Int, String, Int, String, String,
        String, String, String, String)]
    Await.result(mysqlDb.run(query), timeout)
      .map(x => (NameStringIndexData.apply _).tupled(x))
  }

  def version: String = {
    val query = sql"""SHOW VARIABLES LIKE "%version%";""".as[(String, String)]
    Await.result(mysqlDb.run(query), timeout)
      .map { case (k, v) => k + " " + v }
      .mkString("\n")
  }

  override def close(): Unit = mysqlDb.close()
}
