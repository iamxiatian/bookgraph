package nlp.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.model.{FormData, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import nlp.lexical.Segment

import scala.concurrent.duration._


/**
  * Restful API的服务接口
  */
object HttpApiServer extends JsonSupport {

  val settings = CorsSettings.defaultSettings.copy(
    allowedOrigins = HttpOriginRange.*
  )

  def start(implicit system: ActorSystem, port: Int) = {
    implicit val timeout = Timeout(5.seconds)

    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher


    val route =
      (path("") & get) {
        redirect("index.html", StatusCodes.MovedPermanently)
      } ~
        (path("api" / "segment") & post) {
          entity(as[FormData]) { data =>
            val text = data.fields.get("text")
            complete(Segment.segmentAsString(text.getOrElse("")))
          }
        } ~
        get {
          // 所有其他请求，都直接访问web目录中的对应内容
          //getFromResourceDirectory("web")
          getFromDirectory("web")
        }

    Http().bindAndHandle(route, "0.0.0.0", port)
  }
}
