package io.scalac.service

import io.scalac.exceptions.GithubAppException
import javax.inject.Inject
import io.scalac.model.{Contributor, Repo}

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import cats._
import cats.implicits._
import cats.data._
import cats.syntax._
import scala.concurrent.impl.Promise

// TODO: RankOrganizationContributors.rank
class GetOrganizationContributorsRanking @Inject()(getOrganizationRepos: GetOrganizationRepos, getContributors: GetContributors) {

  // TODO: rename to rank
  def start(organizationName: String): Future[Either[GithubAppException, List[Contributor]]] =
    for {
      numberOfRepositoriesPages <- getOrganizationRepos.getNumberOfRepositoryPages(organizationName)
      repos <- getOrganizationRepos.repos(organizationName, numberOfRepositoriesPages)
      contributors <- getContributors.getAllProjectsContributors(organizationName, repos)
    } yield contributors.map( // TODO: make function for this reduce
      _.groupMapReduce(_.name)(_.contributions)(_ + _).toList
        .map(tup2 => Contributor(tup2._1, tup2._2))
        .sortWith(_.contributions > _.contributions))
}