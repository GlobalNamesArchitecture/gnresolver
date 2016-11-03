package org.globalnames
package resolver
package api

import CrossMapSearcher.{Source, Result, Target}
import spray.json.{DefaultJsonProtocol, _}

trait CrossMapProtocols extends DefaultJsonProtocol with NullOptions {
  case class CrossMapRequest(dbSinkIds: Seq[Int], localIds: Seq[String])

  implicit val crossMapRequestFormat = jsonFormat2(CrossMapRequest.apply)

  implicit val cmSourceFormat = jsonFormat2(Source.apply)
  implicit val cmTargetFormat = jsonFormat3(Target.apply)
  implicit val cmResultFormat = jsonFormat2(Result.apply)
}
