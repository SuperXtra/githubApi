package io.scalac.service

import io.scalac.config.GithubApiConfig
import mockws.MockWS
import mockws.MockWSHelpers.Action
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Results.NoContent

class GetContributionsTest extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with GivenWhenThen  {

  val input = getClass.getResourceAsStream("github/octokitReposPageOne.json")

//  getNumberOfContributorsPages
//  getContributorsFromResponse(getContributorsResponseFromPage)

  "GetContributors" should {

    "get number of contributors pages" in {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("a ws client that will return response with github's 'Link' header which contain link to last page 5")
      val wsClient = MockWS {
        case (_, _) => Action {
          NoContent.withHeaders("Link" -> "<https://api.github.com/resource?page=2>; rel=\"next\",\n      <https://api.github.com/resource?page=5>; rel=\"last\"")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue

      Then("it should return 5 pages")
      result shouldBe 5
    }

    "get number of contributors pages from incorrect link" in {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("a ws client that will return response with github's 'Link' header which does not contain link to any page")
      val wsClient = MockWS {
        case (_, _) => Action {
          NoContent.withHeaders("Link" -> "<https://api.github.com/organizations/3430433/repos?page=2>; rel=\"next\"")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue

      Then("it should return 1 page")
      result shouldBe 1
    }

    "get number of contributors pages with empty string header" in {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("a ws client that will return response with github's 'Link' header which contain empty string")
      val wsClient = MockWS {
        case (_, _) => Action {
          NoContent.withHeaders("Link" -> "")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue

      Then("it should return 1 pages")
      result shouldBe 1
    }

    "get number of contributors pages when no content" in {

      Given("organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      val config = GithubApiConfig(
        ghToken = "test_token",
        baseUrl = ""
      )

      And("a ws client that will return no content")
      val wsClient = MockWS {
        case (_, _) => Action {
          NoContent
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue

      Then("it should return 1 page")
      result shouldBe 1
    }

  }

}
