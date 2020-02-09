package io.scalac.service

import io.scalac.config.GithubApiConfig
import javax.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsArray, Reads}
import play.api.libs.ws.WSClient
import cats.implicits._
import io.scalac.exceptions.{CouldNotAuthorizeToGithubApi, GithubAppException, GithubPageNotFound, UsedGithubApiQuota}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class Generic @Inject()(ws: WSClient, githubApiConfig: GithubApiConfig, url: GetGithubUrls) {

  val logger: Logger = Logger(this.getClass)

  // TODO: class GetNumberOfPages get(...)
  def QuantityOfPagesToTraverse(url: String): Future[Either[GithubAppException, Int]] =
    ws.url(url)
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map(response =>
        response.status match {
          case Status.OK => {
            response.headers.find {
              case (headerName, _) => headerName.trim == "Link"
            }.flatMap {
              case (_, headerValue) => {
                githubApiConfig.headerRegex.r.findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
              }
            }.getOrElse(1)
            }.asRight
          case Status.NO_CONTENT => 1.asRight
          case Status.NOT_FOUND => GithubPageNotFound.asLeft
          case Status.FORBIDDEN => UsedGithubApiQuota.asLeft
          case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft
        }
      )

  // TODO: class GetDataFromPage .get(...)
  def getFromPage[T](url: String)(implicit reads: Reads[T]): Future[Either[GithubAppException, List[T]]] =
    ws.url(url)
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map { response => {
        response.status match {
          case Status.OK =>
            response.json.as[JsArray]
              .value
              .map(record => record.validate[T].get)
              .toList
              .asRight[GithubAppException]
              .recoverWith[GithubAppException, List[T]] {
                case e: Throwable =>
                  logger.error(s"could not load project for $url", e)
                  List.empty[T].asRight[GithubAppException]
              }
          case Status.NOT_FOUND => GithubPageNotFound.asLeft[List[T]]
          case Status.FORBIDDEN => UsedGithubApiQuota.asLeft[List[T]]
          case Status.UNAUTHORIZED => CouldNotAuthorizeToGithubApi.asLeft[List[T]]
          case _ => List.empty[T].asRight[GithubAppException]
        }
      }
      }
}