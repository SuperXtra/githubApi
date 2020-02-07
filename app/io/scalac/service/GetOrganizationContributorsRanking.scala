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

  val logger: Logger = Logger(this.getClass)

  def start(organizationName: String): Future[Either[GithubAppException,List[Contributor]]] = {
    for {
      numberOfRepositoriesPages: Either[GithubAppException, Int] <- getOrganizationRepos.getNumberOfRepositoryPages(organizationName)
      repos: Either[GithubAppException, List[Repo]] <- getOrganizationRepos.repos(organizationName, numberOfRepositoriesPages)
      contributors <- getContributors.getAllProjectsContributors(organizationName, repos)
    } yield contributors
  }



//      .groupMapReduce(_.name)(_.contributions)(_ + _).toList
//      .map(rec => Contributor(rec._1, rec._2))
//      .sortWith(_.contributions > _.contributions)
}