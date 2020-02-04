package com.scalac.service

import com.scalac.model.{Repository, User}
import javax.inject.Inject
import play.api.libs.json.{JsArray, Json, OFormat}
import play.api.{Configuration, Logger}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.matching.Regex

class CallExternalApi @Inject()(ws: WSClient, config: Configuration) {

  import CallExternalApi._

  val token: String = config.get[String]("GH_TOKEN")
  val organization = "octokit"
  val baseUrl = config.get[String]("BASE_URL")

  //  val organization = "freecodecamp"  // not working, at one point is throwing exception because result is empty and cannot prase Json
  // TO DO


  def findNumberOfPages(linkHeader: String): String = {
    val pattern: Regex = """(?=>; rel="next")(?:.*)(?<=page=)(.*)(?=>; rel="last")""".r
    if (linkHeader.equals("1")) "1" else pattern.findAllIn(linkHeader).group(1)
  }

  def start = {

    val repositoriesUrl = s"${baseUrl}/orgs/${organization}/repos"
    def repositoryUsersContribution(repositoryName: String) = s"${baseUrl}/repos/${organization}/${repositoryName}/contributors"

    val repos: List[Repository] = Await.result(downloadRepositories(repositoriesUrl), 10 seconds)

    val a: Seq[Future[List[User]]] = repos.map(x => downloadRepositoryUsersAndActivityCount(repositoryUsersContribution(x.name))(x.name))
    val b: Seq[User] = a.flatMap(x => Await.result(x, 50 seconds))
    val c: Map[String, Long] = b.groupMapReduce(_.login)(x => x.contributions)(_ + _)

    c.toList.sortBy(_._2).reverse

  }

  def pageCountWithFirstPage(url: String): (String, Future[WSResponse]) = {
    val firstPage: Future[WSResponse] = ws.url(url).withMethod("GET").addHttpHeaders("Authorization" -> s"token ${token}").get()
    val headerWithTotalPagesCount = Await.result(firstPage.map(x => x.header("Link")), 5 seconds)
    val totalPagesCount = findNumberOfPages(headerWithTotalPagesCount.getOrElse("1"))
    (totalPagesCount, firstPage)
  }

  def downloadRepositories(url: String): Future[List[Repository]] = {
    val pageCountWithFirstOne = pageCountWithFirstPage(url)

    @tailrec
    def go(counter: Int, acc: Future[List[Repository]]): Future[List[Repository]] = {
      if (counter == pageCountWithFirstOne._1.toInt) acc
      else go(counter + 1, acc.flatMap(x => fetchFromPageAsync((counter + 1).toString)(url).map(y => x ++ y)))
    }

    go(1, responseToRepo(pageCountWithFirstOne._2))
  }

  def downloadRepositoryUsersAndActivityCount(url: String)(repositoryName: String) = {
    val pageCountWithFirstOne = pageCountWithFirstPage(url)

    def go(counter: Int, acc: Future[List[User]]): Future[List[User]] = {
      if (counter == pageCountWithFirstOne._1.toInt) acc
      else go(counter + 1, acc.flatMap(x => fetchFromPageAsync2((counter + 1).toString, repositoryName)(url).map(y => x ++ y)))
    }

    go(1, responseToUser(pageCountWithFirstOne._2))
  }

  def responseToRepo(response: Future[WSResponse]): Future[List[Repository]] = {
    response
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[Repository].get)
      }.map(_.toList)
  }

  def responseToUser(response: Future[WSResponse]): Future[List[User]] = {
    response
      .map { res =>
        res.json.as[JsArray]
          .value.map(record => record.validate[User].get)
      }.map(_.toList)
  }

  def fetchFromPageAsync(page: String)(url: String): Future[List[Repository]] = {
    val response = ws.url(s"${url}?page=${page}")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${token}").get()

    responseToRepo(response)
  }

  def fetchFromPageAsync2(page: String, repositoryName: String)(url: String): Future[List[User]] = {
    val response = ws.url(s"${url}?page=${page}")
      .withMethod("GET")
      .addHttpHeaders("Authorization" -> s"token ${token}").get()

    responseToUser(response)
  }
}

object CallExternalApi {
  implicit val newImplicit: OFormat[Repository] = Json.format[Repository]
  implicit val user: OFormat[User] = Json.format[User]
}
