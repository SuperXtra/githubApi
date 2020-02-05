package com.scalac.service

import com.scalac.config.GithubApiConfig
import com.scalac.model.{Contributor, Project}
import javax.inject.Inject
import play.api.libs.json.{JsArray, JsPath, Json, OFormat, Reads, Writes}
import play.api.{Configuration, Logger}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import cats._
import cats.implicits._
import cats.syntax._

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.matching.Regex
import play.api.libs.functional.syntax._
import play.api.Logger


class GetOrganizationContributorsRanking @Inject()(ws: WSClient, githubApiConfig: GithubApiConfig, getOrganizationProjectService: GetOrganizationProjectsService) {

  val logger: Logger = Logger(this.getClass())
  implicit val contributionFormat: Reads[Contributor] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    ) (Contributor)

  //  val itemsOnPage = 30 // TODO: Make config from it

  def start(organizationName: String): Future[List[Contributor]] = {
    val results: Future[List[Contributor]] = for {
      numberOfPages <- getOrganizationProjectService.getNumberOfProjectPages(organizationName)
      projects <- getOrganizationProjectService.lists(organizationName, numberOfPages)
      contributions <- getAllProjectsContributors(organizationName, projects)
    } yield contributions
      .groupMapReduce(_.name)(_.contributions)(_ + _).toList
      .map(rec => Contributor(rec._1, rec._2))
      .sortWith(_.contributions > _.contributions)

    results
  }


  // TODO: Move these methods below to new service `GetContributors`
  def getAllProjectsContributors(organizationName: String, projects: List[Project]): Future[List[Contributor]] = {
    logger.warn(projects.toString())
    val projectContributors = projects.map(project => getProjectContributors(organizationName, project.name))
    Future.sequence(projectContributors).map(_.flatten)
  }

  // TODO: Maybe make config from it
  val GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER: Regex = "page=([0-9]+)>; rel=\\\"last\\\"".r

  // TODO: Document it. Write some comment why whe do this this way

  def getContributorsFromResponse(request: Future[WSResponse]): Future[List[Contributor]] = {
    request
      .map { res => {
        res.json.as[JsArray]
          .value.map(record => record.validate[Contributor].get)
      }
      }.map(_.toList)
  }

  def getProjectContributors(organizationName: String, projectName: String): Future[List[Contributor]] = {
    val numberOfContributorsPages = getOrganizationProjectService.getNumberOfContributorsPages(organizationName, projectName)

    val otherPages: Future[List[Future[WSResponse]]] = numberOfContributorsPages.map(pages =>
      (for (page <- 1 to pages) yield getContributorsResponseFromPage(organizationName, projectName, page)).toList
    )

    for {
      otherPagesContributors <- otherPages.flatMap {
        otherPagesData => otherPagesData.map(responseF => getContributorsFromResponse(responseF)).flatSequence
      }
    } yield otherPagesContributors

  }

  // TODO: Think about creating config for urls
  def getContributorsResponseFromPage(organizationName: String, projectName: String, page: Int): Future[WSResponse] =
    ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$projectName/contributors?page=$page")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
}