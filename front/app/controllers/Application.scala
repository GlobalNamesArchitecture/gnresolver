package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import javax.inject._

import models._
import play.api.libs.json._
import play.api.libs.ws._

import scalaz.{Inject => _, _}
import Scalaz._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Formatters.responseFormat

class Application @Inject() (wsClient: WSClient,
                             config: play.api.Configuration) extends Controller {
  val searchForm = Form("name_query" -> text)
  val apiHostname = config.getString("resolver.api.hostname").get

  def search(query: Option[String], page: Option[Int]): Action[AnyContent] =
    Action.async { implicit req =>
      val drop = page.map { _ * 10 }.orZero
      wsClient
        .url(s"http://$apiHostname/api/search?v=${query.get}&take=10&drop=$drop")
        .withRequestTimeout(15.seconds)
        .get
        .map { resp =>
          resp.json.validate[Response] match {
            case JsSuccess(ns, _) =>
              val totalPagesCount = ns.total / 10 + 1
              val pageUrls = (1 until totalPagesCount).map { page =>
                (page, "?q=" + query.orZero + "&page=" + page)
              }.toMap
              Ok(views.html.index(query.orZero, page, pageUrls, ns.some))
            case e: JsError => BadRequest(e.toString)
          }
        }
    }

  def index(): Action[AnyContent] = Action {
    Ok(views.html.index())
  }
}
