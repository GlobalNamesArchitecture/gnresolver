package org.globalnames
package resolver
package api

import java.util.UUID

import akka.http.scaladsl.unmarshalling.Unmarshaller
import Resolver.NameRequest
import model.{DataSource, MatchType, Matches, NameStringIndex, LocalId}
import spray.json.{DefaultJsonProtocol, _}

trait NamestringsProtocols extends DefaultJsonProtocol {
  implicit def uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case _ => deserializationError("String expected")
    }
  }

  case class VernacularResponseItem(name: String, language: Option[String],
                                    locality: Option[String], countryCode: Option[String])

  case class VernacularResponse(dataSourceId: Int, values: Seq[VernacularResponseItem])

  case class Response(page: Int, perPage: Int, total: Long, localId: Option[LocalId],
                      suppliedNameString: Option[String], matches: Seq[ResponseItem])

  case class ResponseItem(nameStringUuid: UUID, nameString: String,
                          canonicalNameUuid: Option[UUID], canonicalName: Option[String],
                          surrogate: Option[Boolean],
                          dataSourceId: Int, dataSourceTitle: String,
                          taxonId: String, globalId: Option[String],
                          classificationPath: Option[String], classificationPathIds: Option[String],
                          classificationPathRanks: Option[String],
                          vernaculars: Seq[VernacularResponse],
                          matchType: MatchType)

  def result(matches: Matches, page: Int, perPage: Int): Response = {
    val items = matches.matches.map { m =>
      val vernaculars = m.vernacularStrings.groupBy { _._2.dataSourceId }.map { case (dsi, xs) =>
        val vris = xs.map { case (vs, vsi) =>
          VernacularResponseItem(vs.name, vsi.language, vsi.locality, vsi.countryCode) }
        VernacularResponse(dsi, vris)
      }.toSeq

      ResponseItem(m.nameString.name.id, m.nameString.name.value,
        m.nameString.canonicalName.map { _.id }, m.nameString.canonicalName.map { _.value },
        m.nameString.surrogate,
        m.dataSource.id, m.dataSource.title,
        m.nameStringIndex.taxonId, m.nameStringIndex.globalId,
        m.nameStringIndex.classificationPath, m.nameStringIndex.classificationPathIds,
        m.nameStringIndex.classificationPathRanks,
        vernaculars,
        m.matchType)
    }
    Response(page, perPage, matches.total, matches.localId, matches.suppliedNameString, items)
  }

  implicit val vernacularResponseItemFormat = jsonFormat4(VernacularResponseItem.apply)
  implicit val vernacularResponseFormat = jsonFormat2(VernacularResponse.apply)
  implicit val responseItemFormat = jsonFormat14(ResponseItem.apply)
  implicit val responseFormat = jsonFormat6(Response.apply)
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID): JsString = JsString(x.toString)
    def read(value: JsValue): UUID = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
  implicit def matchTypeFormat: JsonFormat[MatchType] = new JsonFormat[MatchType] {
    def write(x: MatchType): JsString = JsString(x.toString)

    def read(value: JsValue): MatchType = value match {
      case JsString(js) => js match {
        case "None" => MatchType.None
        case "Fuzzy" => MatchType.Fuzzy
        case "ExactNameMatchByUUID" => MatchType.ExactNameMatchByUUID
        case "ExactCanonicalNameMatchByUUID" => MatchType.ExactCanonicalNameMatchByUUID
        case x => deserializationError("Expected MatchType as JsString, but got " + x)
      }
      case x => deserializationError("Expected MatchType as JsString, but got " + x)
    }
  }
  implicit val nameStringIndexFormat = jsonFormat12(NameStringIndex.apply)
  implicit val dataSourceFormat = jsonFormat3(DataSource.apply)
  implicit val nameRequestFormat = jsonFormat2(NameRequest.apply)

  implicit def unmarshalStringJson2NameRequest: Unmarshaller[String, Seq[NameRequest]] =
    Unmarshaller.strict((_: String).parseJson match {
      case JsArray(objs: Vector[JsValue]) =>
        objs.map { case obj: JsObject => nameRequestFormat.read(obj) }
      case obj: JsObject => Seq(nameRequestFormat.read(obj))
    })

  implicit def unmarshalStringJson2SeqInt: Unmarshaller[String, Vector[Int]] =
    Unmarshaller.strict((_: String).parseJson match {
      case JsArray(xs: Vector[JsValue]) => xs.map { case x: JsNumber => x.value.toInt }
    })
}
