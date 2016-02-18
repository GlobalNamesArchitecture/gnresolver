package org.globalnames
package resolver

import java.util.UUID

import org.globalnames.parser.ScientificNameParser.{instance => snp}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

object Importer {
  val logger = LoggerFactory.getLogger("org.globalnames.resolver.Importer")

  final private val pattern =
    "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x"

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
    sys.props("importType") match {
      case "test_connection" => testConnection()
      case "name_strings" => importNameStrings()
      case "name_strings_indicies" => importNameStringsIndicies()
      case _ => System.err.println("No appropriate `importType` is provided")
    }
  }

  def testConnection(): Unit = {
    val timeout: Duration = sys.props("timeout").toInt.seconds
    val step              = sys.props("step").toInt
    logger.info(s"""Started `test_connection` with parameters:
                    |step: $step
                    |timeout: $timeout
                    |""".stripMargin)
    val mysqlDAL      = new MySqlDAL(timeout, step)
    logger.info(s"mysql version: ${mysqlDAL.version}")
    val postgresqlDAL = new PostgreSqlDAL(timeout, step)
    logger.info(s"postgresql version: ${postgresqlDAL.version}")
  }

  def importNameStringsIndicies(): Unit = {
    var idx               = sys.props("startIndex").toInt
    val step              = sys.props("step").toInt
    val timeout: Duration = sys.props("timeout").toInt.seconds

    logger.info(s"""Started `name_strings_indicies` with parameters:
                   |index: $idx
                   |step: $step
                   |timeout: $timeout sec""".stripMargin)

    val mysqlDal     = new MySqlDAL(timeout, step)
    val postgesqlDal = new PostgreSqlDAL(timeout, step)

    try {
      while (idx < mysqlDal.nameStringIndicesCount / step) {
        try {
          val data = mysqlDal.nameStringsIndiciesData(idx)

          // TODO: Add created_at, updated_at, synonym to INSERT query
          data.foreach { case nameStringIndexData =>
            try {
              postgesqlDal.insertNameStringIndicies(nameStringIndexData)
            } catch {
              case e: Exception =>
                logger.error(s"""Exception for: $nameStringIndexData
                                |${e.toString}""".stripMargin)
            }
          }
          idx += 1
          logger.info(s"processed: ${idx * step}")
        } catch {
          case e: Exception =>
            logger.error(s"Exception: ${e.toString}")
        }
      }
    } finally {
      mysqlDal.close()
      postgesqlDal.close()
    }

    logger.info(s"""Completed import `name_strings_indicies`""")
  }

  def importNameStrings(): Unit = {
    var idx               = sys.props("startIndex").toInt
    val step              = sys.props("step").toInt
    val timeout: Duration = sys.props("timeout").toInt.seconds

    logger.info(
      s"""Started import `name_strings` with parameters:
         |index: $idx
         |step: $step
         |timeout: $timeout""".stripMargin)

    val mysqlDal     = new MySqlDAL(timeout, step)
    val postgesqlDal = new PostgreSqlDAL(timeout, step)

    try {
      val gen = UuidGenerator()
      while (idx < mysqlDal.nameStringsCount / step) {
        mysqlDal.nameStringsData(idx).foreach { case (id, name, uuidStr) =>
          try {
            val parsed = snp.fromString(name)
            val canonical = parsed.canonized(showRanks = false).getOrElse("")
            val nameUuid = uuidConvert(uuidStr)
            val parsedUuid = UUID.fromString(parsed.input.id)
            val canonicalUuid = gen.generate(canonical)
            postgesqlDal.writeNameString(parsedUuid, nameUuid, name,
                                         canonicalUuid, canonical)
            if (nameUuid != parsedUuid) {
              logger.error(s"Unmatched UUIDs: $id | $name | $canonical")
            }
          } catch {
            case e: Exception =>
              logger.error(s"Exception: ${e.toString}")
          }
        }
        idx += 1
        logger.info(s"processed: ${idx * step}")
      }
    } finally {
      mysqlDal.close()
      postgesqlDal.close()
    }
    logger.info("Completed import `name_strings`")
  }
}
