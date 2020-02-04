package com.scalac.model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Repository(id: Int, name: String)

//object Repository {
//  implicit val readRepository: Reads[Repository] = (
//    (JsPath \ "id").read[Int] and
//      (JsPath \ "name").read[String])(Repository.apply _)
//
////  implicit val testRead = (
////    (__ \ "id").read[Int] and
////      (__ \ "name").read[String]
////  )(Repository)
//}