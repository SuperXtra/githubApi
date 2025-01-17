package io.scalac.service

import io.scalac.config.GithubApiConfig
import javax.inject.Inject

case class GetGithubUrls @Inject()(config : GithubApiConfig) {
  private lazy val baseUrl = config.baseUrl

  def organizationUrl(organizationName: String) = s"$baseUrl/orgs/$organizationName/repos"
  def reposUrl(organizationName:String, page: Int) = s"$baseUrl/orgs/$organizationName/repos?page=$page"

  // TODO:  rename repository to repo + url
  def repositoryContributorsPage(organizationName: String, repositoryName: String, page: Int) =
    s"$baseUrl/repos/$organizationName/$repositoryName/contributors?page=$page"

  // TODO:  rename repository to repo with url sufix -> numberOfRepoPagesUrl
  def fetchPaginationValueForRepository(organizationName: String, repositoryName: String) =s"$baseUrl/repos/$organizationName/$repositoryName/contributors"

  // TODO: numberOfContributosPagesUrl

}