package com.scalac.service

import com.scalac.config.GithubApiConfig
import com.scalac.model.{Contributor, Project}
import javax.inject.Inject
import play.api.libs.json.{JsArray, JsPath, Json, OFormat, Reads}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex


class GetOrganizationProjectsService @Inject()(ws: WSClient, githubApiConfig: GithubApiConfig){

  val GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER : Regex = "page=([0-9]+)>; rel=\\\"last\\\"".r


  implicit val projectFormat: OFormat[Project] = Json.format[Project]


  // TODO: Document it. Write some comment why whe do this this way
  def getNumberOfContributorsPages(organizationName: String, repositoryName: String) : Future[Int] =
    ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$repositoryName/contributors")
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map(
        response => response.headers.find {
          case (headerName, _) => headerName.trim == "Link"
        }.flatMap {
          case (_, headerValue) => {
            GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER
              .findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
          }
        }.getOrElse(1)
      )

  def getNumberOfProjectPages(organizationName: String): Future[Int] =
    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos")
      .withMethod("HEAD")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map(
        response => response.headers.find {
          case (headerName, _) => headerName.trim == "Link"
        }.flatMap {
          case (_, headerValue) => {
            GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER
              .findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
          }
        }.getOrElse(1)
      )

  def lists(organizationName: String, pages: Int): Future[List[Project]] = {
    val projectPages = (1 to pages).map(page => getProjectsFromPage(organizationName, page))
    Future.sequence(projectPages).map(_.flatten.toList)
  }

  private def getProjectsFromPage(organizationName: String, page: Int): Future[List[Project]] =
    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[Project].get)
      }.map(_.toList)


}
