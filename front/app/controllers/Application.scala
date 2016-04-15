package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import org.globalnames.parser.ScientificNameParser.{instance => snp}
import javax.inject._

import models._
import play.api.libs.json._
import play.api.libs.ws._

import scalaz.{Inject => _, _}
import Scalaz._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Formatters.responseFormat

class Application @Inject() (wsClient: WSClient) extends Controller {
  val searchForm = Form("name_query" -> text)

  def parsePost() = Action(parse.form(searchForm)) { implicit rs =>
    Redirect(routes.Application.search(rs.body.some, None))
  }

  def search(query: Option[String], page: Option[Int]) = Action.async { implicit req =>
    val drop = page.map { _ * 10 }.orZero
    wsClient
      .url(s"http://gnresolver.globalnames.org/api/search?v=${query.get}&take=10&drop=$drop")
      .withRequestTimeout(3.seconds)
      .get
      .map { resp =>
        resp.json.validate[Response] match {
          case JsSuccess(ns, _) =>
            Ok(views.html.index(query.orZero, ns.some))
          case e: JsError => BadRequest(e.toString)
        }
      }
  }

  def index() = Action {
    Ok(views.html.index())
  }
}
