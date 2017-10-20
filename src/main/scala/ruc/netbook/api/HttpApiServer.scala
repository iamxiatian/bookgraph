package nlp.api

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{parameter, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import nlp.api.actor.WebExtractorActor
import nlp.deeplearning.Clustering
import nlp.extract.{EntityExtractor, StockExtractor}
import nlp.lexical.Segment
import spray.json.{JsArray, JsNumber, JsString}

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


    val webExtractorActor = system.actorOf(Props[WebExtractorActor], "webExtractorActor")

    val route =
      (path("") & get) {
        redirect("index.html", StatusCodes.MovedPermanently)
      } ~
        (path("extract_stock_code") & post) {
          entity(as[FormData]) {
            data =>
              val text = data.fields.get("text")
              println(s"try to extract stock code from $text")
              val body = StockExtractor.extractCodes(text.getOrElse("")).mkString("\n")
              complete(body)
          }
        } ~
        (path("extract_stock_code2") & post) {
          entity(as[String]) {
            text =>
              println(s"try to extract stock code from $text")
              val body = StockExtractor.extractCodes(text).mkString("\n")
              complete(body)
          }
        } ~
        (path("api" / "sim_words") & get & parameter('w.as[String] ? "中国")) {
          word =>
            val jsonValue = Clustering.word2vec.distance(List(word))
              .map { case (w, score) => JsArray(JsString(w), JsNumber(score)) }
              .toJson

            complete(HttpEntity(ContentTypes.`application/json`, jsonResultOk(jsonValue)))
        } ~
        (path("api" / "clustering") & post) {
          entity(as[FormData]) { data =>
            val text = data.fields.get("text")
            println(s"try to do clustering from $text")
            val body = Clustering.clustering(text.getOrElse(""), 5)
            complete(body)
          }
        } ~
        (path("api" / "entity_extract") & post) {
          entity(as[FormData]) { data =>
            val text = data.fields.get("text")
            complete(EntityExtractor.extractAsJson(text.getOrElse("")))
          }
        } ~
        (path("api" / "segment") & post) {
          entity(as[FormData]) { data =>
            val text = data.fields.get("text")
            complete(Segment.segmentAsString(text.getOrElse("")))
          }
        } ~
        (path("api" / "page_extract") & get & parameter('u.as[String], 't ? "article")) {
          //匹配api/extract_article?u=xxxxx&t=article, url参数的结果会自动赋予url，t的默认参数为article
          (url: String, urlType: String) =>
            onSuccess(webExtractorActor ? (url, urlType == "hub")) {
              case content: String =>
                complete(HttpEntity(ContentTypes.`application/json`, content))
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
        } ~
        EntityRoute.entityNetworkRoutes ~
        CrawlerRoute.routes ~
        get {
          // 所有其他请求，都直接访问web目录中的对应内容
          //getFromResourceDirectory("web")
          getFromDirectory("web")
        }

    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  //
  //  def showEntityNetworkRoute: Route =
  //    (path("api" / "huaxiang")
  //      & parameter('w.as[List[String]])
  //      & get & cors(settings)) {
  //      words =>
  //        onSuccess(EntityGraph.getSubNetwork(EntityGraph.prefix, words)) {
  //          case result: Map[String, Map[String, Double]] =>
  //            //            val data:List[JsObject] = result.map {
  //            //              case (category: String, items: Map[String, Double]) =>
  //            //                //把items中的（名称->score）转换为一个列表对象
  //            //                items.map {
  //            //                  case (name: String, score: Double) =>
  //            //                    JsObject(
  //            //                      "name" -> JsString(name),
  //            //                      "value" -> JsNumber(score),
  //            //                      "symbolSize" -> JsNumber(score),
  //            //                      "category" -> JsString(
  //            //                        if(category == EntityGraph.PERSON) "人物"
  //            //                        else if(category == EntityGraph.SPACE) "地点"
  //            //                        else if(category == EntityGraph.ORGANIZATION) "机构"
  //            //                        else "主题"
  //            //                      )
  //            //                    )
  //            //                }
  //            //            }.flatten.toList
  //
  //            def makeJsArray(category: String) =
  //              JsArray(
  //                result.getOrElse(category, Map.empty[String, Double])
  //                  .map {
  //                    case (name: String, score: Double) =>
  //                      JsObject(
  //                        "name" -> JsString(name),
  //                        "score" -> JsNumber(score)
  //                      ).asInstanceOf[JsValue]
  //                  }.toVector
  //              )
  //
  //            val json = JsObject(
  //              "words" -> JsArray(words.map(JsString(_))),
  //              "persons" -> makeJsArray(EntityGraph.PERSON),
  //              "spaces" -> makeJsArray(EntityGraph.SPACE),
  //              "organizations" -> makeJsArray(EntityGraph.ORGANIZATION),
  //              "keywords" -> makeJsArray(EntityGraph.KEYWORD)
  //            )
  //
  //            complete(
  //              HttpEntity(ContentTypes.`application/json`, json.prettyPrint)
  //            )
  //          case _ =>
  //            complete(StatusCodes.InternalServerError)
  //        }
  //    }

}
