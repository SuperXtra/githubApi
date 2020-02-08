package io.scalac.service

import io.scalac.config.GithubApiConfig
import io.scalac.exceptions._
import io.scalac.model.Repo
import javax.inject.Inject
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.JsArray
import play.api.libs.ws.WSClient
import cats._
import cats.data._
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetOrganizationRepos @Inject()(ws: WSClient, config: GithubApiConfig, url: GetGithubUrls) extends Logging {

  def repos(organizationName: String, pages: Either[GithubAppException, Int]): Future[Either[GithubAppException,List[Repo]]] = {
    pages match {
      case Left(value) => Future(value.asLeft)
      case Right(pages) =>
        (1 to pages).toList.flatTraverse(round => Nested(getReposFromPage(organizationName, round)).map(_.toList)).value
    }
  }

  def getNumberOfRepositoryPages(organizationName: String): Future[Either[GithubAppException, Int]] =
    ws.url(url.organizationUrl(organizationName))
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${config.ghToken}").get()
      .map( response =>
        response.status match {
          case Status.OK => {
            response.headers.find {
              case (headerName, _) => headerName.trim == "Link"
            }.flatMap {
              case (_, headerValue) =>
                config.headerRegex.r.findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
            }.getOrElse(1)
          }.asRight
          case Status.NO_CONTENT => 1.asRight
          case Status.NOT_FOUND => GithubPageNotFound.asLeft
          case Status.FORBIDDEN => UsedGithubApiQuota.asLeft
          case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft
        }
      )

  def getReposFromPage(organizationName: String, page: Int): Future[Either[GithubAppException, List[Repo]]] =
    ws.url(url.reposUrl(organizationName, page))
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${config.ghToken}").get()
      .map( response =>
          response.status match {
        case Status.OK =>
          response.json.as[JsArray]
            .value.map(record => record.validate[Repo].get)
            .toList.asRight[GithubAppException]
            .recoverWith{
              case error: Throwable =>
                logger.error(s"could not load project for $organizationName page $page", error)
                List.empty[Repo].asRight
            }
        case Status.NO_CONTENT => List.empty[Repo].asRight
        case Status.NOT_FOUND => GithubPageNotFound.asLeft
        case Status.FORBIDDEN => UsedGithubApiQuota.asLeft
        case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft
        }
      )
}
