package com.scalac.service

import com.scalac.config.GithubApiConfig
import com.scalac.model.{Contributor, Organization, Project}
import javax.inject.Inject
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import cats._
import cats.implicits._
import cats.syntax._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.matching.Regex

class GetOrganizationContributorsRanking @Inject()(ws: WSClient,githubApiConfig: GithubApiConfig) {

  implicit val organizationFormat: OFormat[Organization] = Json.format[Organization]
  implicit val projectFormat: OFormat[Project] = Json.format[Project]
  implicit val contributionFormat: OFormat[Contributor] = Json.format[Contributor]

  val itemsOnPage = 30 // TODO: Make config from it

  def start(organizationName: String) : Future[List[Contributor]] = {
    val test = for {
      numberOfPages <- getNumberOfProjectPages(organizationName)
      projects <- getOrganizationProjects(organizationName, numberOfPages / itemsOnPage)
      contributions <- getAllProjectsContributors(organizationName, projects)
    } yield contributions // TODO: Add reduce

    println(Await.result(test, 5.second)) // TODO: Remove (only for tests)
    test
  }

  // TODO: Move these 3 methods to GetOrganizationProjects service (then rename getOrganizationProjects to `list`)
  def getNumberOfProjectPages(organizationName: String): Future[Int] =
    ws.url(s"${githubApiConfig.baseUrl}/users/$organizationName")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map { res =>
        res.json.as[Organization].public_repos // TODO: Do something with the name public_repos
      }

  def getOrganizationProjects(organizationName: String, pages: Int): Future[List[Project]] = {
    val projectPages = (1 to pages).map(page => getProjectsFromPage(organizationName, page))
    Future.sequence(projectPages).map(_.flatten.toList)
  }

  def getProjectsFromPage(organizationName: String, page: Int): Future[List[Project]] =
    ws.url(s"${githubApiConfig.baseUrl}/orgs/$organizationName/repos?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[Project].get)
      }.map(_.toList)


  // TODO: Move these methods below to new service `GetContributors`
  def getAllProjectsContributors(organizationName: String, projects: List[Project]) : Future[List[Contributor]] = {
    val projectContributors = projects.map(project => getProjectContributors(organizationName, project.name))
    Future.sequence(projectContributors).map(_.flatten)
  }

  // TODO: Maybe make config from it
  val GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER : Regex = "page=([0-9]+)>; rel=\\\"last\\\"".r

  // TODO: Document it. Write some comment why whe do this this way
  def getNumberOfContributorsPages(firstPage: Future[WSResponse]) : Future[Int] =  firstPage.map(
    response => response.headers.find {
      case (headerName, _) => headerName.trim == "Link"
    }.flatMap {
      case (_, headerValue) => {
        GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER
          .findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
      }
    }.getOrElse(0)
  )

  def getContributorsFromResponse(request: Future[WSResponse]) : Future[List[Contributor]] =
    request
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[Contributor].get)
      }.map(_.toList)

  def getProjectContributors(organizationName: String, projectName: String) : Future[List[Contributor]] = {
    val firstPage : Future[WSResponse] = getContributorsResponseFromPage(organizationName, projectName, 0)
    val numberOfContributorsPages = getNumberOfContributorsPages(firstPage)

    val otherPages: Future[List[Future[WSResponse]]] = numberOfContributorsPages.map(pages =>
      (for (page <- 1 to pages) yield getContributorsResponseFromPage(organizationName, projectName, page)).toList
    )

    for {
      firstPageContributors <- getContributorsFromResponse(firstPage)
      otherPagesContributors <- otherPages.flatMap{
        otherPagesData => otherPagesData.map(responseF => getContributorsFromResponse(responseF)).flatSequence
      }
    } yield firstPageContributors ::: otherPagesContributors

  }

  // TODO: Think about creating config for urls
  def getContributorsResponseFromPage(organizationName: String, projectName: String, page: Int): Future[WSResponse] =
    ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$projectName/contributors?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()

}