package io.scalac.service

import javax.inject.Inject
import io.scalac.model.Contributor

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger


class GetOrganizationContributorsRanking @Inject()(getOrganizationRepos: GetOrganizationRepos, getContributors: GetContributors) {

  val logger: Logger = Logger(this.getClass)

  def start(organizationName: String): Future[List[Contributor]] =
    for {
      numberOfRepositoriesPages <- getOrganizationRepos.getNumberOfRepositoryPages(organizationName)
      repos <- getOrganizationRepos.repos(organizationName, numberOfRepositoriesPages)
      contributors <- getContributors.getAllProjectsContributors(organizationName, repos)
    } yield contributors
      .groupMapReduce(_.name)(_.contributions)(_ + _).toList
      .map(rec => Contributor(rec._1, rec._2))
      .sortWith(_.contributions > _.contributions)
}