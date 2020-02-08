package io.scalac.controller

import io.scalac.exceptions
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
class GithubApi @Inject()(cached: Cached, cc: ControllerComponents, getOrganizationContributors: GetOrganizationContributorsRanking)
  extends AbstractController(cc) with Logging {

  implicit val contributorsWrites: Writes[Contributor] =
    (contribution: Contributor) => Json.obj(
      "name" -> contribution.name,
      "contributions" -> contribution.contributions
    )

  @ApiOperation(
    nickname = "listContributors",
    value = "List all contributors",
    notes = "Returns a list of contributors sorted in ascending order",
    response = classOf[Contributor],
    responseContainer = "List",
    httpMethod = "GET"
  )
  def index(org_name: String) =
  cached.status(_ => "/resource/" + org_name, 200, 60) // TODO: must be confiurable + use named arugments
  {
    Action.async {
      for {
        contributors <- getOrganizationContributors.start(org_name)
      } yield contributors match {
        case Left(value) => value match {
          case exceptions.GithubPageNotFound => NotFound(Json.toJson(value.toString)) // TODO do not monkey http description, try to get rid off exceptions
          case exceptions.UsedGithubApiQuota => Forbidden(Json.toJson(value.toString))
          case exceptions.CouldNotAuthorizeToGithubApi => Unauthorized(Json.toJson(value.toString))
        }
        case Right(value) => {
          val result: List[Contributor] = value.groupMapReduce(_.name)(_.contributions)(_ + _).toList
            .map(tup2 => Contributor(tup2._1, tup2._2))
            .sortWith(_.contributions > _.contributions)
          Ok(Json.toJson(result))
        }
      }
    }
  }
}


