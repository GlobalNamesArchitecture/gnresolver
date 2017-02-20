package org.globalnames
package resolver
package api

import model.{Matches, VernacularString, VernacularStringIndex}
import Materializer.Parameters
import sangria.schema._
import sangria.marshalling.sprayJson._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scalaz._
import Scalaz._
import java.util.UUID

import org.globalnames.resolver.Resolver.NameRequest

object SchemaDefinition extends DefaultJsonProtocol {
  val nameStringsMaxCount = 1000
  implicit val nameRequestFormat: RootJsonFormat[NameRequest] =
    jsonFormat2(Resolver.NameRequest.apply)

  val DataSource = ObjectType(
    "DataSource", fields[Unit, model.DataSource](
        Field("id", IntType, resolve = _.value.id)
      , Field("title", StringType, resolve = _.value.title)
    )
  )

  val Name = ObjectType(
    "Name", fields[Unit, model.Name](
        Field("id", IDType, resolve = _.value.id.toString)
      , Field("name", StringType, resolve = _.value.value)
    )
  )

  val Classification = ObjectType(
    "Classification", fields[Unit, model.NameStringIndex](
        Field("path", OptionType(StringType), resolve = _.value.classificationPath)
      , Field("pathIds", OptionType(IDType), resolve = _.value.classificationPathIds)
      , Field("pathRanks", OptionType(StringType), resolve = _.value.classificationPathRanks)
    )
  )

  val Vernacular = ObjectType(
    "Vernacular", fields[Unit, (VernacularString, VernacularStringIndex)](
        Field("id", IDType, resolve = _.value._1.id.toString)
      , Field("name", StringType, resolve = _.value._1.name)
      , Field("dataSourceId", IntType, resolve = _.value._2.dataSourceId)
    )
  )

  val ResponseItem = ObjectType(
    "ResponseItem", fields[GnRepo, model.Match](
        Field("name", Name, resolve = _.value.nameString.name)
      , Field("canonicalName", OptionType(Name), resolve = _.value.nameString.canonicalName)
      , Field("surrogate", OptionType(BooleanType), resolve = _.value.nameString.surrogate)
      , Field("dataSource", OptionType(DataSource), resolve = _.value.dataSource)
      , Field("taxonId", IDType, resolve = _.value.nameStringIndex.taxonId)
      , Field("globalId", OptionType(IDType), resolve = _.value.nameStringIndex.globalId)
      , Field("localId", OptionType(IDType), resolve = _.value.nameStringIndex.localId)
      , Field("classification", OptionType(Classification), resolve = _.value.nameStringIndex)
      , Field("vernaculars", ListType(Vernacular), resolve = _.value.vernacularStrings)
      , Field("matchType", StringType, resolve = _.value.matchType.toString)
      // , Field("prescore", StringType, resolve = ???)
    )
  )

  val Response = ObjectType(
    "Response", fields[GnRepo, model.Matches](
        Field("total", LongType, None, resolve = _.value.total)
      , Field("suppliedInput", OptionType(StringType), None, resolve = _.value.suppliedInput)
      , Field("suppliedId", OptionType(IntType), None, resolve = _.value.suppliedId)
      , Field("results", ListType(ResponseItem), None, resolve = _.value.matches)
    )
  )

  val Id = Argument("id", StringType)
  val Page = Argument("page", OptionInputType(IntType), 0)
  val PerPage = Argument("perPage", OptionInputType(IntType), nameStringsMaxCount)
  val WithSurrogates = Argument("withSurrogates", OptionInputType(BooleanType), false)
  val WithVernaculars = Argument("withVernaculars", OptionInputType(BooleanType), false)
  val NameRequestObj: InputObjectType[NameRequest] = InputObjectType[NameRequest]("name", List(
    InputField("value", StringType),
    InputField("suppliedId", OptionInputType(IntType))
  ))
  val NamesRequest = Argument("names", ListInputType(NameRequestObj))
  val DataSourceIds = Argument("dataSourceIds", OptionInputType(ListInputType(IntType)))

  val QueryType = ObjectType(
    "Query", fields[GnRepo, Unit](
        Field("name_string", Response,
          arguments = List(Id, Page, PerPage, WithSurrogates, WithVernaculars),
          resolve = ctx => ctx.withArgs(Id, Page, PerPage, WithSurrogates, WithVernaculars) {
            (id, page, perPage, withSurrogates, withVernaculars) =>
              ctx.ctx.nameStringByUuid(id, page, perPage, withSurrogates, withVernaculars)
          }
        )
      , Field("name_resolvers", ListType(Response),
          arguments = List(NamesRequest, DataSourceIds, Page, PerPage, WithSurrogates,
                           WithVernaculars),
          resolve = ctx => ctx.withArgs(NamesRequest, DataSourceIds, Page, PerPage,
                                        WithSurrogates, WithVernaculars) {
            (names, dataSourceIds, page, perPage, withSurrogates, withVernaculars) =>
              ctx.ctx.nameStrings(names.take(nameStringsMaxCount), dataSourceIds, page, perPage,
                                  withSurrogates, withVernaculars)
          }
        )
    )
  )

  val schema = Schema(QueryType)
}

case class GnRepo(facetedSearcher: FacetedSearcher, resolver: Resolver)
                 (implicit executor: ExecutionContextExecutor) {
  def nameStringByUuid(uuid: String, page: Int, perPage: Int,
                       withSurrogates: Boolean, withVernaculars: Boolean): Future[Matches] = {
    val uuidParsed = UUID.fromString(uuid)
    val params = Parameters(page, perPage, withSurrogates, withVernaculars)
    facetedSearcher.findNameStringByUuid(uuidParsed, params)
  }

  def nameStrings(names: Seq[NameRequest], dataSourceIds: Option[Seq[Int]],
                  page: Int, perPage: Int,
                  withSurrogates: Boolean, withVernaculars: Boolean): Future[Seq[Matches]] = {
    val params = Parameters(page, perPage, withSurrogates, withVernaculars)
    resolver.resolveExact(names, dataSourceIds.map { _.toVector }.orZero, params)
  }
}
