package io.scalac.service

import java.net.URL

import io.scalac.config.GithubApiConfig
import io.scalac.model.Repo
import mockws.MockWS
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import play.api.mvc.Results._
import play.api.libs.json._
import mockws.MockWSHelpers._

import scala.io.Source

class GetOrganizationReposTest
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with GivenWhenThen {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(Span(500, Millis), Span(10, Millis))

  "GetOrganizationRepos" should {

    "get repos from page" in {

      Given("some organization name")
      val organizationName = "test_organization"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("organization has 2 repositories")
      val source = Source.fromFile("test/resources/github/test.json")
      val file = try source.mkString finally source.close()

      And("a ws client that will return organization projects in response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(file)).withHeaders("Content-Type" -> "application/json")
        }
      }


      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config)
        .getReposFromPage(
          organizationName = organizationName,
          page = 3)
        .futureValue

      Then("it should return 2 repositories")
      val expectedRepos = List(
        Repo(1, "amazing project"),
        Repo(2, "another project")
      )
      result shouldBe expectedRepos
    }

    "get repos from first page, real world json" in {

      Given("some organization name")
      val organizationName = "test_organization"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("organization has 30 repositories")
      val source = Source.fromFile("test/resources/github/octokitReposPageOne.json")
      val file = try source.mkString finally source.close()

      And("a ws client that will return organization projects in response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(file)).withHeaders("Content-Type" -> "application/json")
        }
      }


      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config)
        .getReposFromPage(
          organizationName = organizationName,
          page = 3)
        .futureValue

      Then("it should return 30 repositories")

      result.size shouldBe 30
    }

    "get repos from second page, real world json" in {

      Given("some organization name")
      val organizationName = "test_organization"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("organization has 15 repositories")
      val source = Source.fromFile("test/resources/github/octokitReposPageTwo.json")
      val file = try source.mkString finally source.close()

      And("a ws client that will return organization projects in response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(file)).withHeaders("Content-Type" -> "application/json")
        }
      }


      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config)
        .getReposFromPage(
          organizationName = organizationName,
          page = 3)
        .futureValue

      Then("it should return 15 repositories")

      result.size shouldBe 15
    }
  }
}