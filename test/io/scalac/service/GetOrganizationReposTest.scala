package io.scalac.service


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
import io.scalac.model.Repo
import scala.io.Source

class GetOrganizationReposTest
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with GivenWhenThen {

  "GetOrganizationRepos" should {

    "get repos from page" in new Context {

      Given("some organization name")
      val organizationName = "test_organization"

      And("organization has 2 repositories")
      val organizationReposJson =
        """
          |[
          |  {
          |    "id": 1,
          |    "someSpam": "test",
          |    "name": "amazing project"
          |  },
          |  {
          |    "id": 2,
          |    "name": "another project",
          |    "not_needed_field": 1234
          |  }
          |]
        """.stripMargin

      And("a ws client that will return 2 organization's repositoris in the response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(organizationReposJson)).withHeaders("Content-Type" -> "application/json")
        }
      }

      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config, urlService)
        .getReposFromPage(
          organizationName = organizationName,
          page = 3)
        .futureValue

      Then("it should return 2 repositories")
      val expectedRepos = List(
        Repo(1, "amazing project"),
        Repo(2, "another project")
      )
      result.getOrElse(Nil) shouldBe expectedRepos
    }

    "get repos from first page, real world json" in new Context with ExampleReposPageOne {

      Given("some organization name")
      val organizationName = "test_organization"

      And("a ws client that will return one page of 30 repos in the response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(reposPageOne)).withHeaders("Content-Type" -> "application/json")
        }
      }

      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config, urlService)
        .getReposFromPage(
          organizationName = organizationName,
          page = 3)
        .futureValue

      Then("it should return 30 repositories")
      result.getOrElse(Nil).size shouldBe 30
    }

    "get repos from first page, real world json [generic test]" in new Context with ExampleReposPageOne {

      Given("some organization name")
      val organizationName = "test_organization"

      And("a ws client that will return one page of 30 repos in the response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(reposPageOne)).withHeaders("Content-Type" -> "application/json")
        }
      }

      When("getting repositories from some page")
      val result = new Generic(wsClient, config, urlService)
        .getFromPage[Repo](urlService.reposUrl(
          organizationName = organizationName,
          page = 3))
        .futureValue

      Then("it should return 30 repositories")
      result.getOrElse(Nil).size shouldBe 30
    }

    "get repos from second page, real world json" in new Context with ExampleReposPageTwo {

      Given("some organization name")
      val organizationName = "test_organization"

      And("a ws client that will return a page with 15 repos in the response")
      val wsClient = MockWS {
        case (_, _) => Action {
          Ok(Json.parse(reposPageTwo)).withHeaders("Content-Type" -> "application/json")
        }
      }

      When("getting repositories from some page")
      val result = new GetOrganizationRepos(wsClient, config, urlService)
        .getReposFromPage(organizationName,3)
        .futureValue

      Then("it should return 15 repositories")
      result.getOrElse(Nil).size shouldBe 15
    }
  }

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(Span(1000, Millis), Span(100, Millis))

  trait Context {

    import io.scalac.service.GetContributors._

    val config = GithubApiConfig(
      ghToken = "test_token",
      baseUrl = "",
      headerRegex = "page=([0-9]+)>; rel=\"last\"",
      cacheTime = 60
    )

    val urlService = GetGithubUrls(config)

  }

  trait ExampleReposPageOne {

    val source = Source.fromFile("test/resources/github/octokitReposPageOne.json")
    val reposPageOne = try source.mkString finally source.close()
  }

  trait ExampleReposPageTwo {

    val source = Source.fromFile("test/resources/github/octokitReposPageTwo.json")
    val reposPageTwo = try source.mkString finally source.close()
  }
}