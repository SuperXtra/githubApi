package io.scalac.service


import io.scalac.config.GithubApiConfig
import mockws.MockWS
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import play.api.mvc.Results._
import mockws.MockWSHelpers._

class GetContributionsTest extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with GivenWhenThen  {

  "GetContributors" should {

    "get number of contributors pages" in new Context {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      And("a ws client that will return response with github's 'Link' header which contain link to last page 5")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok.withHeaders("Link" -> "<https://api.github.com/resource?page=2>; rel=\"next\",\n      <https://api.github.com/resource?page=5>; rel=\"last\"")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config, urlService)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue.getOrElse(0)

      Then("it should return 5 pages")
      result shouldBe 5
    }

    "get number of contributors pages from incorrect link" in new Context {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      And("a ws client that will return response with github's 'Link' header which does not contain link to any page")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok.withHeaders("Link" -> "<https://api.github.com/organizations/3430433/repos?page=2>; rel=\"next\"")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config, urlService)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue.getOrElse(0)

      Then("it should return 1 page")
      result shouldBe 1
    }

    "get number of contributors pages with empty string header" in new Context {

      Given("octokit organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      And("a ws client that will return response with github's 'Link' header which contain empty string")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok.withHeaders("Link" -> "")
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config, urlService)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue.getOrElse(0)

      Then("it should return 1 pages")
      result shouldBe 1
    }

    "get number of contributors pages when no content" in new Context {

      Given("organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      And("a ws client that will return no content")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok
        }
      }

      When("getting number of project pages")
      val result = new GetContributors(wsClient, config, urlService)
        .getNumberOfContributorsPages(
          organizationName = organizationName,
          repositoryName = repositoryName)
        .futureValue.getOrElse(0)

      Then("it should return 1 page")
      result shouldBe 1
    }


    "get number of contributors pages when no generic service test" in new Context {

      Given("organization and repository names")
      val organizationName = "test_organization"
      val repositoryName = "test_ProjectContributors"

      And("a ws client that will return no content")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok
        }
      }

      When("getting number of project pages")
      val result = new Generic(wsClient, config, urlService)
        .QuantityOfPagesToTraverse(urlService.fetchPaginationValueForRepository(organizationName, repositoryName))
        .futureValue.getOrElse(0)

      Then("it should return 1 page")
      result shouldBe 1
    }

  }

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(Span(1000, Millis), Span(100, Millis))

  trait Context {

    val input = getClass.getResourceAsStream("github/octokitReposPageOne.json")

    val config = GithubApiConfig(
      ghToken = "test_token",
      baseUrl = "",
      headerRegex = "page=([0-9]+)>; rel=\"last\"",
      cacheTime = 60
    )

    val urlService = GetGithubUrls(config)

  }

}