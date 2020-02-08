package io.scalac.exceptions

sealed trait GithubAppException

case object GithubPageNotFound extends GithubAppException
case object UsedGithubApiQuota extends GithubAppException
case object CouldNotAuthorizeToGithubApi extends GithubAppException

