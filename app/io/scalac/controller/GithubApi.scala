package io.scalac.controller

import io.scalac.config.GithubApiConfig
import io.scalac.exceptions
import io.scalac.exceptions.{CouldNotAuthorizeToGithubApi, GithubAppException, GithubPageNotFound, UsedGithubApiQuota}
import io.scalac.model.Contributor
import io.scalac.service.GetOrganizationContributorsRanking
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import io.swagger.annotations._
import play.api.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.cache._

@Api(value = "Contributors")
@Singleton
class GithubApi @Inject()(cached: Cached, cc: ControllerComponents, config: GithubApiConfig,  getOrganizationContributors: GetOrganizationContributorsRanking)
  extends AbstractController(cc) with Logging {

  // TODO: swagger might not work with private model
  case class ErrorInfo(message: String)

  implicit val errorInfoWrites: OWrites[ErrorInfo] = Json.writes[ErrorInfo]

  implicit val contributorsWrites: Writes[Contributor] =
    (contribution: Contributor) => Json.obj(
      "name" -> contribution.name,
      "contributions" -> contribution.contributions
    )

  @ApiResponses(Array(
    new ApiResponse(code = 401, message = "Provided token is not valid"),
    new ApiResponse(code = 403, message = "Reached hourly request limit"),
    new ApiResponse(code = 404, message = "Organization not found")))
  @ApiOperation(
    nickname = "listContributors",
    value = "List all contributors",
    notes = "Returns a list of contributors sorted in ascending order",
    response = classOf[Contributor],
    responseContainer = "List",
    httpMethod = "GET"
  )
  def index(org_name: String) =

    cached.status(_ => "/resource/" + org_name, status = 200, config.cacheTime) {
      Action.async {
        for {
          contributors <- getOrganizationContributors.start(org_name)
        } yield contributors match {
          case Left(error: GithubAppException) =>
            error match {
              case GithubPageNotFound => NotFound(Json.toJson(ErrorInfo("organization not found")))
              case UsedGithubApiQuota => Forbidden(Json.toJson(ErrorInfo("you used your github api quota")))
              case CouldNotAuthorizeToGithubApi => Unauthorized(Json.toJson(ErrorInfo("bad token")))
            }
          case Right(value) => Ok(Json.toJson(value))
        }
      }
    }
}

