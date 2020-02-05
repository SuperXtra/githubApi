package com.scalac.controllers

import com.scalac.model.Contributor
import com.scalac.service.GetOrganizationContributorsRanking
import javax.inject._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class GitHubApiController @Inject()(ws: WSClient, cc: ControllerComponents, getOrganizationContributors: GetOrganizationContributorsRanking)
  extends AbstractController(cc) {

  implicit val contributorsWrites: Writes[Contributor] =
    new Writes[Contributor] {
      def writes(contribution: Contributor) = Json.obj(
        "login"  -> contribution.login,
        "contributions" -> contribution.contributions
      )
    }

  def index = Action.async {
    val organizationName = "octokit" // TODO: Make it http param
    for {
      contributors <- getOrganizationContributors.start(organizationName)
    } yield Ok(Json.toJson(contributors))
  }
}


