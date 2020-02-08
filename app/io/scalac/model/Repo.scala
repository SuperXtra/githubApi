package io.scalac.model

import play.api.libs.json.{Json, OFormat}

case class Repo(id: Int, name: String)

object Repo {
  implicit val projectFormat: OFormat[Repo] = Json.format[Repo]

}
