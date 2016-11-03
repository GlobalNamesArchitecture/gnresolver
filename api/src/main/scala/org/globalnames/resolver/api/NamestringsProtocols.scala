package org.globalnames
package resolver
package api

import java.util.UUID

import akka.http.scaladsl.unmarshalling.Unmarshaller
import Resolver.NameRequest
import model.{DataSource, Kind, Match, Matches, Name, NameString, NameStringIndex}
import spray.json.{DefaultJsonProtocol, _}

trait NamestringsProtocols extends DefaultJsonProtocol with NullOptions {
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID): JsString = JsString(x.toString)
    def read(value: JsValue): UUID = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
  implicit object KindJsonFormat extends RootJsonFormat[Kind] {
    def write(x: Kind): JsString = JsString(x.toString)
    def read(value: JsValue): Kind = value match {
      case JsString(js) => js match {
        case "None" => Kind.None
        case "Fuzzy" => Kind.Fuzzy
        case "ExactNameMatchByUUID" => Kind.ExactNameMatchByUUID
        case "ExactCanonicalNameMatchByUUID" => Kind.ExactCanonicalNameMatchByUUID
        case x => deserializationError("Expected Kind as JsString, but got " + x)
      }
      case x => deserializationError("Expected Kind as JsString, but got " + x)
    }
  }
  implicit val nameFormat = jsonFormat2(Name.apply)
  implicit val nameStringFormat = jsonFormat3(NameString.apply)
  implicit val nameStringIndexFormat = jsonFormat12(NameStringIndex.apply)
  implicit val dataSourceFormat = jsonFormat3(DataSource.apply)
  implicit object MatchJsonFormat extends RootJsonFormat[Match] {
    def write(m: Match): JsObject = JsObject(
        "nameStringUuid" -> JsString(m.nameString.name.id.toString)
      , "nameString" -> JsString(m.nameString.name.value)
      , "surrogate" -> m.nameString.surrogate.map { x => JsBoolean(x) }.getOrElse(JsNull)
      , "canonicalNameUuid" -> m.nameString.canonicalName.map { x => JsString(x.id.toString) }
        .getOrElse(JsNull)
      , "canonicalName" -> m.nameString.canonicalName.map { x => JsString(x.value) }
        .getOrElse(JsNull)
      , "dataSourceId" -> JsNumber(m.dataSource.id)
      , "dataSourceTitle" -> JsString(m.dataSource.title)
      , "taxonId" -> JsString(m.nameStringIndex.taxonId)
      , "globalId" -> m.nameStringIndex.globalId.map { x => JsString(x) }.getOrElse(JsNull)
      , "classificationPath" -> m.nameStringIndex.classificationPath.map { x => JsString(x) }
        .getOrElse(JsNull)
      , "classificationPathIds" -> m.nameStringIndex.classificationPathIds.map { x => JsString(x) }
        .getOrElse(JsNull)
      , "classificationPathRanks" -> m.nameStringIndex.classificationPathRanks
        .map { x => JsString(x) }.getOrElse(JsNull)
      , "vernacular" ->
        JsArray(m.vernacularStrings.map { case (v, vs) => JsString(v.name) }.toVector)
      , "kind" -> KindJsonFormat.write(m.kind)
    )

    def read(m: JsValue): Match =
      m.asJsObject.getFields("nameStringUuid", "nameString", "surrogate",
        "canonicalNameUuid", "canonicalName",
        "dataSourceId", "dataSourceTitle", "kind",
        "taxonId", "globalId", "classificationPath",
        "classificationPathIds", "classificationPathRanks") match {
        case Seq(JsString(nsUuid), JsString(ns), surrogateJs, cnUuidJs, cnJs, JsNumber(dsi),
        JsString(dst), kindJs, JsString(taxonId), globalId,
        classificationPath, classificationPathIds, classificationPathRanks) =>
          val canonical =
            for { cnUuidOpt <- cnUuidJs.convertTo[Option[UUID]]
                  cnOpt <- cnJs.convertTo[Option[String]] } yield Name(cnUuidOpt, cnOpt)
          val surrogateOpt = surrogateJs.convertTo[Option[Boolean]]
          val nameString = NameString(Name(UUID.fromString(nsUuid), ns), canonical, surrogateOpt)
          val dataSource = DataSource(dsi.toInt, dst, "")
          val nameStringIndex = {
            val cpOpt = classificationPath.convertTo[Option[String]]
            val cpiOpt = classificationPathIds.convertTo[Option[String]]
            val cprOpt = classificationPathRanks.convertTo[Option[String]]
            NameStringIndex(dataSource.id, nameString.name.id, url = None,
              taxonId = taxonId, globalId = globalId.convertTo[Option[String]],
              localId = None, nomenclaturalCodeId = None,
              rank = None, acceptedTaxonId = None,
              classificationPath = cpOpt,
              classificationPathIds = cpiOpt,
              classificationPathRanks = cprOpt)
          }
          Match(nameString, dataSource, nameStringIndex, Seq(), kindJs.convertTo[Kind])
        case _ => deserializationError("Match expected")
      }
  }
  implicit val matchesFormat = jsonFormat4(Matches.apply)
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
