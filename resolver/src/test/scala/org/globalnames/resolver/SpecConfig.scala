package org.globalnames.resolver

import java.io.File

import org.scalatest._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process.Process

trait SpecConfig extends WordSpec with Matchers with OptionValues
                    with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures
                    with PatienceConfiguration {
  protected val log: Logger = LoggerFactory.getLogger(getClass)

  implicit val defaultPatience: PatienceConfig = {
    // scalastyle:off magic.number
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(5, Millis))
    // scalastyle:on magic.number
  }

  protected def seed(appEnv: String, specName: String) = {
    log.info(Process(command = s"rake db:seed RACK_ENV=$appEnv SPEC_NAME=$specName",
                     cwd = new File("../db-migration")).!!)
  }
}
