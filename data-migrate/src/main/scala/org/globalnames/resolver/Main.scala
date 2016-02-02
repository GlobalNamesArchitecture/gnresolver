package org.globalnames.resolver

import java.util.UUID

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends UUIDPlainImplicits {

  val logger = LoggerFactory.getLogger("org.globalnames.resolver.Main")

  final private val pattern =
    "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x"
  final private val step = 1000
  final private val timeout: Duration = 30.seconds

  private def uuidConvert(uuidStr: String) = {
    var uuidParts = ArrayBuffer(BigInt(uuidStr).toByteArray: _*)
    if (uuidParts.size > 16) {
      uuidParts = uuidParts.takeRight(16)
    } else if (uuidParts.size < 16) {
      uuidParts.prependAll(Array.fill(16 - uuidParts.size)(0.toByte))
    }
    UUID.fromString(pattern.format(uuidParts: _*))
  }

  def main(args: Array[String]): Unit = {
    var idx = args(0).toInt

    logger.info(s"Started at index: $idx")

    val postgresqlDb = Database.forConfig("postgresql")
    val mysqlDb      = Database.forConfig("mysql")

    try {
      val count = {
        val query = sql"SELECT count(*) FROM name_strings;".as[Int].head
        Await.result(mysqlDb.run(query), timeout)
      }
      while (idx < count / step) {
        logger.info(s"processed: ${idx * step}")

        val data = {
          val query =
            sql"""SELECT id, `name`, uuid
                  FROM name_strings
                  LIMIT ${idx * step}, $step;""".as[(Int, String, String)]
          Await.result(mysqlDb.run(query), timeout)
        }
        data.foreach { case (id, name, uuidStr) =>
          try {
            val canonical =
              snp.fromString(name).canonized(showRanks = false).getOrElse("")
            val dbUuid = uuidConvert(uuidStr)
            val query = sqlu"""
              INSERT INTO gni.name_strings (id, name, canonical)
              VALUES ($dbUuid, $name, $canonical);"""
            Await.result(postgresqlDb.run(query), timeout)
            if (dbUuid != UUID.fromString(snp.fromString(name).input.id)) {
              logger.error(s"Unmatched UUIDs: $id | $name | $canonical")
            }
          } catch {
            case e: Exception =>
              logger.error(s"Exception: ${e.toString}")
          }
        }
        idx += 1
      }
    } finally {
      mysqlDb.close()
      postgresqlDb.close()
    }
  }
}
