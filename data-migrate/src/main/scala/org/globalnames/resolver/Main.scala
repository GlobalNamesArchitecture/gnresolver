package org.globalnames.resolver

import java.util.UUID

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends UUIDPlainImplicits {

  final private val pattern =
    "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x"
  final private val step = 1000
  final private val timeout: Duration = 10.seconds

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
    val postgresqlDb = Database.forConfig("postgresql")
    val mysqlDb      = Database.forConfig("mysql")

    try {
      val count = {
        val query = sql"SELECT count(*) FROM name_strings;".as[Int].head
        Await.result(mysqlDb.run(query), timeout)
      }
      for (i <- 0 to count / step) {
        println(s"processed: ${i * step}")

        val data = {
          val query =
            sql"""SELECT id, `name`, uuid
                  FROM name_strings
                  LIMIT ${i * step}, $step;""".as[(Int, String, String)]
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
            println(s"Added: ($id, $name, $normalized)")
          } catch {
            case e: Exception => System.err.println(e.toString)
          }

        }
      }
    } finally {
      mysqlDb.close()
      postgresqlDb.close()
    }
  }
}
