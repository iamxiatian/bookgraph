package ruc.bookgraph.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  def jsonResultOk(data: JsValue) = Map(
    "status" -> JsString("OK"),
    "data" -> data).toJson.prettyPrint

  def jsonResultError(message: String) = Map(
    "status" -> "ERROR",
    "msg" -> message).toJson.prettyPrint
}