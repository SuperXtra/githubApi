package com.scalac.controllers

import com.scalac.model.Contributor
import com.scalac.service.GetOrganizationContributorsRanking
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

@Singleton
class GithubApiController @Inject()(ws: WSClient, cc: ControllerComponents, getOrganizationContributors: GetOrganizationContributorsRanking)
  extends AbstractController(cc) {

  implicit val contributorsWrites: Writes[Contributor] =
    (contribution: Contributor) => Json.obj(
      "name" -> contribution.name,
      "contributions" -> contribution.contributions
    )

//  "twbs"

  def index = Action.async {
    val organizationName = "octokit" // TODO: Make it http param
    for {
      contributors <- getOrganizationContributors.start(organizationName)
    } yield Ok(Json.toJson(contributors))
  }
}



