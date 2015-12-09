import java.io._

import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main {
  final val pattern = "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x"

  def main(args: Array[String]): Unit = {
    val db = Database.forConfig("mysql")
    val bw = new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream("name_strings.cql"), "UTF-8"))
    try {
      bw.write("USE gni_keyspace;\n")
      val r =
        sql"""SELECT id, `name`, normalized, uuid
              FROM gni.name_strings;""".as[(Int, String, String, String)]
      val v = db.stream(r).foreach { case (id, name, normalized, uuidStr) =>
        if (id % 10000 == 0) {
          println(s"processed: $id")
        }

        var uuidParts = ArrayBuffer(BigInt(uuidStr).toByteArray: _*)
        if (uuidParts.size > 16) {
          uuidParts = uuidParts.takeRight(16)
        } else if (uuidParts.size < 16) {
          uuidParts.prependAll(Array.fill(16 - uuidParts.size)(0.toByte))
        }
        val uuid = pattern.format(uuidParts: _*)
        val nameEscaped = name.replaceAll("'", "''")
        val normalizedNameEscaped = normalized.replaceAll("'", "''")
        val res = "INSERT INTO normalized_strings (id,name,normalized,uuid) " +
          s"""VALUES ($id,'$nameEscaped','$normalizedNameEscaped',$uuid);"""
        bw.write(res + "\n")
      }
      Await.result(v, Duration.Inf)
    } finally {
      db.close
      bw.close
    }
  }
}
