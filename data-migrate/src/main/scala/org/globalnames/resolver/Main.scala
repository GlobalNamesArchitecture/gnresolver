package org.globalnames.resolver

import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

object Main {
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
    pattern.format(uuidParts: _*)
  }

  def main(args: Array[String]): Unit = {
    val mysqlDb = Database.forConfig("mysql")
    try {
      val count = {
        val request = sql"SELECT count(*) FROM name_strings;".as[Int].head
        Await.result(mysqlDb.run(request), timeout)
      }
      for (i <- 0 to count / step) {
        println(s"processed: ${i * step}")

        val data = {
          val request =
            sql"""SELECT id, `name`, normalized, uuid
                  FROM name_strings
                  LIMIT ${i * step}, $step;""".as[(Int, String, String, String)]
          Await.result(mysqlDb.run(request), timeout)
        }
        data.foreach { case (id, name, normalized, uuidStr) =>
          val insertStmnt = s"""
            |INSERT INTO normalized_strings (id,name,normalized,uuid)
            |VALUES($id, '$name', '$normalized', ${uuidConvert(uuidStr)})
            |""".stripMargin
          println(insertStmnt)
        }
      }
    } finally {
      mysqlDb.close
    }
  }
}
