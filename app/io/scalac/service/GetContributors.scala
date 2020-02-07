package io.scalac.service

import cats.data.{EitherT, Nested}
import io.scalac.model.{Contributor, Repo}
import javax.inject.Inject
import play.api.libs.json.{JsArray, JsPath, Reads}

import scala.concurrent.Future
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.{WSClient, WSResponse}
import io.scalac.config.GithubApiConfig
import io.scalac.exceptions.{Forbidden, GithubAppException, ResourceNotFound, Unauthorized}
import play.api.Logger
import play.api.http.Status
import play.api.libs.functional.syntax._
import cats._
import cats.implicits._

class GetContributors @Inject()(ws: WSClient, githubApiConfig: GithubApiConfig){

  val logger: Logger = Logger(this.getClass)
  val GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER: Regex = "page=([0-9]+)>; rel=\\\"last\\\"".r

//  def getAllProjectsContributors(organizationName: String, projects: Either[GithubAppException,List[Repo]]): Future[Either[GithubAppException,List[Contributor]]] = {
//
//    for {
//      repos <- EitherT.fromEither[Future](projects)
//      repo <- repos
//      pages <- EitherT(getNumberOfContributorsPages(organizationName, repo.name))
//      page <- 1 to pages
//      contributors <- EitherT(getContributorsFromPage(organizationName, repo.name, page))
//    } yield contributors.value
//  }
//
//  /***
//   *
//   * @param organizationName name of organization
//   * @param repositoryName name of repository to fetch contributors
//   * @return quantity of paginated pages to fetch data
//   *
//   *         According to github API the best way to retrieve information about amount of pages available
//   *         for repository is to access it from header(Link). Information is accessed using regular
//   *         expression.
//   */
//  def getNumberOfContributorsPages(organizationName: String, repositoryName: String): Future[Either[GithubAppException, Int]] =
//    ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$repositoryName/contributors")
//      .withMethod("HEAD")
//      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
//      .map( response =>
//        response.status match {
//          case Status.OK => {
//            response.headers.find {
//              case (headerName, _) => headerName.trim == "Link"
//            }.flatMap {
//              case (_, headerValue) => {
//                GITHUB_NUMBER_OF_PAGES_FROM_LINK_HEADER.findFirstMatchIn(headerValue.toString()).map(_.group(1).toInt)
//              }
//            }.getOrElse(1)
//            }.asRight
//          case Status.NOT_FOUND => ResourceNotFound.asLeft
//          case Status.FORBIDDEN => Forbidden.asLeft
//          case Status.UNAUTHORIZED => Unauthorized.asLeft
//        }
//      )
//
//  def getContributorsFromPage(organizationName: String, repositoryName: String, page: Int): Future[Either[GithubAppException,List[Contributor]]] = {
//    import GetContributors._
//
//    ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$repositoryName/contributors?page=$page")
//      .withMethod("GET")
//      .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()
//      .map { response => {
//        response.status match {
//          case Status.OK => {
//            response.json.as[JsArray]
//              .value.map(record => record.validate[Contributor].get).toList.asRight
//            //            .recover{
//            //              case e: Throwable =>
//            //                logger.error(s"could not load project for $organizationName page $page", e)
//            //            Nil
//            //            }
//          }
//          case Status.NOT_FOUND => ResourceNotFound.asLeft
//          case Status.FORBIDDEN => Forbidden.asLeft
//          case Status.UNAUTHORIZED => Unauthorized.asLeft
//        }
//      }
//      }
//  }

    def getAllProjectsContributors(organizationName: String, projects: Either[GithubAppException,List[Repo]]): Future[Either[GithubAppException,List[Contributor]]] = {
      projects match {
        case Left(value) => Future(value.asLeft)
        case Right(projects) => {
          val projectContributors = projects.map(project => getProjectContributors(organizationName, project.name))
          Future.sequence(projectContributors).map(_.flatten.asRight)
        }
      }


    }

    def getProjectContributors(organizationName: String, repositoryName: String): Future[List[Contributor]] = {
      val numberOfContributorsPages = getNumberOfContributorsPages(organizationName, repositoryName)

      val pages: Future[List[Future[WSResponse]]] = numberOfContributorsPages.map(pages =>
        (for (page <- 1 to pages) yield getContributorsResponseFromPage(organizationName, repositoryName, page)).toList
      )

      for {
        allPagesContributors <- pages.flatMap {
          otherPagesData => otherPagesData.map(responseF => getContributorsFromResponse(responseF)).flatSequence
        }
      } yield allPagesContributors

    }

    /***
     *
     * @param organizationName name of organization
     * @param repositoryName name of repository to fetch contributors
     * @return quantity of paginated pages to fetch data
     *
     *         According to github API the best way to retrieve information about amount of pages available
     *         for repository is to access it from header(Link). Information is accessed using regular
     *         expression.
     */
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


    def getContributorsResponseFromPage(organizationName: String, repositoryName: String, page: Int): Future[WSResponse] =
      ws.url(s"${githubApiConfig.baseUrl}/repos/$organizationName/$repositoryName/contributors?page=$page")
        .withMethod("GET")
        .addHttpHeaders("Authorization" -> s"token ${githubApiConfig.ghToken}").get()


    def getContributorsFromResponse(response: Future[WSResponse]): Future[List[Contributor]] = {
      import GetContributors._
      response
        .map { res => {
          res.json.as[JsArray]
            .value.map(record => record.validate[Contributor].get)
        }
        }.map(_.toList)
        .recover {
          case error: Throwable => {
            response.foreach { req =>
              logger.error(s"failed to fetch contributors for request ${req.uri}", error)
            }
            Nil
          }
        }
    }
  }

object GetContributors {
  implicit val contributionFormat: Reads[Contributor] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    ) (Contributor)
}