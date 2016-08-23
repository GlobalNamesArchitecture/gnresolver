package org.globalnames.resolver
package api

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit._

import scala.concurrent.duration.DurationInt

trait ApiSpecConfig {
  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(5.seconds.dilated(system))
}