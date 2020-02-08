package io.scalac.service

import io.scalac.exceptions.GithubAppException
import javax.inject.Inject
import io.scalac.model.{Contributor, Repo}

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import cats.implicits._

import scala.concurrent.impl.Promise



class GetOrganizationContributorsRanking @Inject()(getOrganizationRepos: GetOrganizationRepos, getContributors: GetContributors) {

  def start(organizationName: String): Future[Either[GithubAppException,List[Contributor]]] = {
    for {
      numberOfRepositoriesPages <- getOrganizationRepos.getNumberOfRepositoryPages(organizationName)
      repos <- getOrganizationRepos.repos(organizationName, numberOfRepositoriesPages)
      contributors <- getContributors.getAllProjectsContributors(organizationName, repos)
    } yield contributors
  }
}