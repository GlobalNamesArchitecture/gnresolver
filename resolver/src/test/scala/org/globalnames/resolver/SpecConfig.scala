package org.globalnames.resolver

import java.io.File

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}

import scala.sys.process.Process

trait SpecConfig extends WordSpec with Matchers with OptionValues
                    with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures
                    with PatienceConfiguration {
  protected val log: Logger = LoggerFactory.getLogger(getClass)

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(5, Millis))

  def seed(appEnv: String, specName: String) = {
    println(Process(command = s"rake db:seed APP_ENV=$appEnv SPEC_NAME=$specName",
                    cwd = new File("../db-migration")).!!)
  }
}
