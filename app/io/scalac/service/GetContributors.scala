package io.scalac.service

import javax.inject.Inject
import io.scalac.model.{Contributor, Repo}
import io.scalac.exceptions._
import io.scalac.config.GithubApiConfig
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSClient
import play.api.Logging
import play.api.http.Status
import cats.data._
import cats.implicits._

class GetContributors @Inject()(ws: WSClient, config: GithubApiConfig, url: GetGithubUrls) extends Logging {

  def getAllProjectsContributors(organizationName: String, projects: Either[GithubAppException,List[Repo]]): Future[Either[GithubAppException,List[Contributor]]] = {
    projects match {
      case Left(value) =>Future(value.asLeft)
      case Right(repositories) =>
        val toTransform= repositories.map(repository => {
          getNumberOfContributorsPages(organizationName, repository.name)
            .map(pages => getContributorsFromPages(organizationName, repository.name, pages)).flatten
        })
        transformer(toTransform)
    }
  }

  // TODO @deprecated("use generic version")
  def getNumberOfContributorsPages(organizationName: String, repositoryName: String): Future[Either[GithubAppException, Int]] =

    ws.url(url.fetchPaginationValueForRepository(organizationName, repositoryName))
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${config.ghToken}").get()
      .map(response =>
        response.status match {
          case Status.OK => {
            response.headers.find {
              case (headerName, _) => headerName.trim == "Link"
            }.flatMap {
              case (_, headerValue) => config.headerRegex.r.findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
            }.getOrElse(1)
            }.asRight
          case Status.NO_CONTENT => 0.asRight
          case Status.NOT_FOUND => GithubPageNotFound.asLeft
          case Status.FORBIDDEN => UsedGithubApiQuota.asLeft
          case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft
        }
      )

  def getContributorsFromPages(organizationName: String, repositoryName: String, pages: Either[GithubAppException,Int]): Future[Either[GithubAppException, List[Contributor]]] = {
    pages match {
      case Left(value) => Future(value.asLeft)
      case Right(pages) =>
        val toTransform = (1 to pages).toList.map(page => getContributorsFromPage(organizationName, repositoryName, page).nested.value)// TODO: urlService.url(organizationName, repositoryName, page)
        transformer(toTransform)
    }
  }

  // TODO @deprecated("use generic version")
  def getContributorsFromPage(organizationName: String, repositoryName: String, page: Int): Future[Either[GithubAppException, List[Contributor]]] = {
    import GetContributors._
    ws.url(s"${config.baseUrl}/repos/$organizationName/$repositoryName/contributors?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${config.ghToken}").get()
      .map { response => {
        response.status match {
          case Status.OK =>
            response.json.as[JsArray]
              .value.map(record => record.validate[Contributor].get).toList.asRight[GithubAppException]
              .recoverWith {
                case e: Throwable =>
                  logger.error(s"could not load project for $organizationName page $page", e)
                  List.empty[Contributor].asRight
              }
          case Status.NO_CONTENT => List.empty[Contributor].asRight
          case Status.NOT_FOUND => GithubPageNotFound.asLeft
          case Status.FORBIDDEN => UsedGithubApiQuota.asLeft
          case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft
        }
      }
      }
  }
  def transformer[T](toTransform: List[Future[Either[GithubAppException, List[T]]]]): Future[Either[GithubAppException, List[T]]] = toTransform.flatTraverse(EitherT(_)).value
}

object GetContributors{
  implicit val contributionReads: Reads[Contributor] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    ) (Contributor)
}