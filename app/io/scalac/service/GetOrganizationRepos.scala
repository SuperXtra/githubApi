package io.scalac.service

import io.scalac.config.GithubApiConfig
import io.scalac.exceptions.{Forbidden, GithubAppException, ResourceNotFound, Unauthorized}
import io.scalac.model.Repo
import javax.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsArray, Json, OFormat}
import play.api.libs.ws.WSClient
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

class GetOrganizationRepos @Inject()(ws: WSClient, githubApiConfig: GithubApiConfig){

  val logger: Logger = Logger(this.getClass)
  val GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER: Regex = "page=([0-9]+)>; rel=\\\"last\\\"".r
  implicit val projectFormat: OFormat[Repo] = Json.format[Repo]

  def getNumberOfRepositoryPages(organizationName: String): Future[Either[GithubAppException, Int]] =
    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos")
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map( response =>
        response.status match {
          case Status.OK => {
            response.headers.find {
              case (headerName, _) => headerName.trim == "Link"
            }.flatMap {
              case (_, headerValue) => {
                GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER.findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
              }
            }.getOrElse(1)
          }.asRight
          case Status.NOT_FOUND => ResourceNotFound.asLeft
          case Status.FORBIDDEN => Forbidden.asLeft
          case Status.UNAUTHORIZED => Unauthorized.asLeft
        }
      )

//  def getReposFromPage(organizationName: String, page: Int): Future[Either[GithubAppException,List[Repo]]] =
//    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos?page=$page")
//      .withMethod("GET")
//      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
//      .map( response =>
//          response.status match {
//        case Status.OK =>
//                  response.json.as[JsArray]
//                    .value.map(record => record.validate[Repo].get).toList.asRight
//            .recover{
//              case error: Throwable =>
//                logger.error(s"could not load project for $organizationName page $page", error)
//                                  Nil
//            }
//        case Status.NOT_FOUND => ResourceNotFound.asLeft
//        case Status.FORBIDDEN => Forbidden.asLeft
//        case Status.UNAUTHORIZED => Unauthorized.asLeft
//        }
//      )

  def getReposFromPage(organizationName: String, page: Int): Future[List[Repo]] =
    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[Repo].get)
      }.map(_.toList)
      .recover {
        case error: Throwable =>
          logger.error(s"could not load project for $organizationName page $page", error)
          Nil
      }

  def repos(organizationName: String, pages: Either[GithubAppException, Int]): Future[Either[GithubAppException,List[Repo]]] = {
    pages match {
      case Left(value) => Future(value.asLeft)
      case Right(pages) =>
        val projectPages= (1 to pages).map(page => getReposFromPage(organizationName, page))
        Future.sequence(projectPages).map(_.flatten.toList.asRight)
      }
    }
}
