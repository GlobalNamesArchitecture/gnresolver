package models

import play.api.libs.json._

object Formatters {
  implicit val nameFormat       = Json.format[Name]
  implicit val nameStringFormat = Json.format[NameString]
  implicit val matchFormat      = Json.format[Match]
  implicit val responseFormat   = Json.format[Response]
}
