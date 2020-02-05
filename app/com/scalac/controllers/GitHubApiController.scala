package com.scalac.controllers

import com.scalac.model.{Repository, User}
import com.scalac.service.CallExternalApi
import javax.inject._
import play.api.Configuration
import play.api.mvc._
import play.api.Logger
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json.{JsError, Json, OFormat, Reads}
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.parsing.input.PagedSeq

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(ws: WSClient, cc: ControllerComponents, ss: CallExternalApi)
  extends AbstractController(cc) {


  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  //  implicit val readRepository: Reads[Repository] = (
  //    (JsPath \ "id").read[Int] and
  //      (JsPath \ "name").read[String])(Repository.apply _)
  //
    implicit val abc: OFormat[Repository] = Json.format[Repository]

  implicit val writeRepo: OFormat[User] = Json.format[User]

//  def index = Action.async {
//    ss.downloadRepositoryUsersAndActivityCount("rest.js").map {
//      s => Ok(Json.toJson(s))
//    }
//  }
  def index = Action {
    Ok(Json.toJson(ss.start))
  }
}


