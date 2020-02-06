package io.scalac.controllers

import io.scalac.config.GithubApiConfig
import io.scalac.service.GetOrganizationRepos
import org.specs2.mock.Mockito
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server
import scala.concurrent.Await
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import play.api.libs.ws.WSResponse
import org.scalatest._
import org.scalatestplus.play._
import play.api.http.MimeTypes
import play.api.test._


class GetOrganizationReposSpec extends Specification with Mockito {

  import scala.concurrent.ExecutionContext.Implicits.global

//
//  "GitHubClient" should {
//    "get number of pages" in {
//
//      val response: WSResponse = mock[WSResponse]
//      response.header("Link") returns Some("")
//      Server.withRouterFromComponents() { components =>
//        import Results._
//        import components.{ defaultActionBuilder => Action }
//        {
//          case GET(p"/repositories") =>
//            Action {
//              Ok("ok").withHeaders("Link" -> "<https://api.github.com/organizations/3430433/repos?page=2>; rel=\"next\", <https://api.github.com/organizations/3430433/repos?page=2>; rel=\"last\"")
//            }
//        }
//      } { implicit port =>
//        WsTestClient.withClient { client =>
//          val result = Await.result(new GetOrganizationRepos(client, GithubApiConfig("test", "https://api.github.com")).getNumberOfRepositoryPages("octokit"), 10.seconds)
//          result must_== 2
//        }
//      }
//    }
//  }
//
//  "GitHubClient" should {
//    "get number of pages" in {
//
//      Server.withRouterFromComponents() { components =>
//        import Results._
//        import components.{ defaultActionBuilder => Action }
//        {
//          case GET(p"/repositories") =>
//            Action { res =>
//              Results.Ok.sendResource("github/octokitReposPageOne.json")
//            }
//        }
//      } { implicit port =>
//        WsTestClient.withClient { client =>
//          val result = Await.result(new GetOrganizationRepos(client, GithubApiConfig("test", "https://api.github.com")).getNumberOfRepositoryPages("octokit"), 10.seconds)
//          result must_== 1
//        }
//      }
//    }
//  }
}