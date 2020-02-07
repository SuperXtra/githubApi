package io.scalac.exceptions

sealed trait GithubAppException

case object ResourceNotFound extends GithubAppException
case object Forbidden extends GithubAppException
case object Unauthorized extends GithubAppException