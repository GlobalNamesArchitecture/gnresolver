package org.globalnames.resolver

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest._

trait SpecConfig extends WordSpec with Matchers with OptionValues
                    with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures
                    with PatienceConfiguration {
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(5, Millis))
}
