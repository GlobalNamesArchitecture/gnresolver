package models

import play.api.libs.json._
import java.util.UUID

case class Name(id: UUID, value: String)
case class NameString(name: Name, canonicalName: Name)
case class DataSource(title: String, description: String)
case class Match(nameString: NameString)
case class Response(total: Int, matches: Seq[Match])

object Formatters {
  implicit val nameFormat = Json.format[Name]
  implicit val nameStringFormat = Json.format[NameString]
  implicit val dataSourceFormat = Json.format[DataSource]
  implicit val matchFormat = Json.format[Match]
  implicit val responseFormat = Json.format[Response]
}
