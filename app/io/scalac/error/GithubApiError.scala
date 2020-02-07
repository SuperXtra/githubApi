package io.scalac.error

sealed trait GithubApiError

case object Unauthorized extends GithubApiError
case object NotFound extends GithubApiError
