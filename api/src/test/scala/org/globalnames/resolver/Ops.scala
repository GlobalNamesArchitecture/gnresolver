package org.globalnames
package resolver

import com.typesafe.config.{Config, ConfigObject, ConfigValue}

import scalaz._
import Scalaz._

object Ops {

  implicit class ConfigOps(val underlying: Config) extends AnyVal {
    def getOptionalBoolean(path: String): Option[Boolean] =
      underlying.hasPath(path).option { underlying.getBoolean(path) }

    def getOptionalObject(path: String): Option[ConfigObject] =
      underlying.hasPath(path).option { underlying.getObject(path) }

    def getOptionalConfigValue(path: String): Option[ConfigValue] =
      underlying.hasPath(path).option { underlying.getValue(path) }
  }

}
