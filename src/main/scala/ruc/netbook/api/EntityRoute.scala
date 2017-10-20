package nlp.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, parameter, path, _}
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import nlp.api.HttpApiServer.settings
import nlp.opinion.EntityGraph.{DEFAULT_PREFIX, _}
import spray.json.{JsArray, JsNumber, JsObject, JsString, _}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * 实体关系处理的AKKA-HTTP路由
  *
  * @author Tian Xia
  *         Jun 20, 2017 10:10
  */
object EntityRoute extends JsonSupport {

  case class Relation(source: String, target: String, score: Double)

  private def process(id: Option[String], category: String, count: Int) = {
    val prefix = if (id.isEmpty) DEFAULT_PREFIX else s"${id.get}:"

    val f: Future[Seq[Seq[Relation]]] = getHighEntities(prefix, count).flatMap {
      entities =>
        Future.sequence(
          entities.map { //对每一个entity，获取其对应的邻居实体
            case (entity: String, _: Double) =>
              val entityName = entity.substring(0, entity.length - 2)

              if (category == "MIXED") {
                getNeighbors(prefix, entityName, entity.substring(entity.length - 1)).flatMap(
                  words =>
                    Future.successful(
                      words.map(wordScore =>
                        Relation(entityName, wordScore._1, wordScore._2)).toSeq
                    )
                )
              } else {
                getNeighbors2(prefix, entityName, category, category).flatMap(
                  words =>
                    Future.successful(
                      words.map(wordScore =>
                        Relation(entityName, wordScore._1, wordScore._2)).toSeq
                    )
                )
              }
          }
        )
    }

    onSuccess(f) {
      case relations: Seq[Seq[Relation]] => complete(toJson(relations.flatten))
      case _ => complete(StatusCodes.InternalServerError)
    }
  }

  // 记录每个要删除的ID
  val deletingIds = ListBuffer.empty[String]

  def entityNetworkRoutes: Route =
    (path("api" / "huaxiang" / "person.json") & get
      & parameters('id.as[String] ?, 'count.as[Int] ?) & cors(settings)) {
      (id: Option[String], count: Option[Int]) =>
        process(id, PERSON, count.getOrElse(50))
    } ~ (path("api" / "huaxiang" / "space.json") & get
      & parameters('id.as[String] ?, 'count.as[Int] ?) & cors(settings)) {
      (id: Option[String], count: Option[Int]) =>
        process(id, SPACE, count.getOrElse(50))
    } ~ (path("api" / "huaxiang" / "organization.json") & get
      & parameters('id.as[String] ?, 'count.as[Int] ?) & cors(settings)) {
      (id: Option[String], count: Option[Int]) =>
        process(id, ORGANIZATION, count.getOrElse(50))
    } ~ (path("api" / "huaxiang" / "keyword.json") & get
      & parameters('id.as[String] ?, 'count.as[Int] ?) & cors(settings)) {
      (id: Option[String], count: Option[Int]) =>
        process(id, KEYWORD, count.getOrElse(50))
    } ~ (path("api" / "huaxiang" / "mixed.json") & get
      & parameters('id.as[String] ?, 'count.as[Int] ?) & cors(settings)) {
      (id: Option[String], count: Option[Int]) =>
        process(id, "MIXED", count.getOrElse(50))
    } ~ (path("api" / "huaxiang" / "post_data") & post & cors(settings)) {
      entity(as[String]) {
        text =>
          log.info(s"Received Json:$text")
          try {
            val data = text.parseJson.asJsObject

            val (id, docs) = data.getFields("id", "docs") match {
              case Seq(JsString(id), JsArray(docs)) =>
                //
                val docWords: Vector[Vector[String]] = docs.map {
                  doc =>
                    val wordArray: JsArray = doc.asInstanceOf[JsArray]
                    wordArray.elements.map(
                      wordValue =>
                        wordValue.asInstanceOf[JsString].value
                    )
                }
                (id, docWords)
              case _ => throw new DeserializationException("Not valid format.")
            }

            val autoRemove = data.getFields("rm").headOption
            autoRemove match {
              case Some(JsTrue) =>
                deletingIds += id
                if (deletingIds.length > 20) {
                  //至多保留20个
                  val deletedId = deletingIds.head
                  removeGraph(deletedId).onComplete {
                    case Success(_) =>
                      println(s"Temp task id $deletedId is deleted.")
                    case Failure(e) =>
                      println(e)
                  }
                }
              case _ =>
            }

            onSuccess(createGraphFromDocs(id, docs)) {
              _ => complete(jsonResultOk(JsString("Received.")))
            }
          } catch {
            case e: Exception => complete(jsonResultError(e.toString))
          }
      }
    } ~ (path("api" / "huaxiang" / "remove") & get
      & parameter('id.as[String]) & cors(settings)) {
      id =>
        if (id == "test") {
          complete(jsonResultError(s"不能删除默认测试集合"))
        } else {
          onSuccess(removeGraph(id)) {
            success =>
              if (success)
                complete(jsonResultOk(JsString(s"$id was removed.")))
              else
                complete(jsonResultError(s"$id was removed."))
          }
        }
    }

  /**
    * 删除所有的临时画像任务
    */
  def removeTempIds() = Future.sequence(
    deletingIds.map(removeGraph)
  ).onComplete {
    case Success(_) =>
      println(s"Temp task ids are all deleted.")
    case Failure(e) =>
      println(e)
  }

  /**
    * 变为JSON格式，方便前端展示
    *
    * @param relations
    */
  def toJson(relations: Seq[Relation]) = {
    case class Node(name: String, score: Double)

    //根据Relation生成所有的节点集合
    val duplicatedNodes: Seq[Node] = relations.map(
      relation =>
        List(Node(relation.source, relation.score),
          Node(relation.target, relation.score))
    ).flatten

    //例如：
    // Map(ok -> List(Node(ok,3.0)),
    // hello -> List(Node(hello,3.0), Node(hello,4.0)))
    val groupNodes = duplicatedNodes.groupBy(_.name)

    //获取所有的不重复的节点及出现次数
    val nodes: Vector[JsObject] = groupNodes.map {
      case (name: String, nodes: Seq[Node]) =>
        //Node(name, nodes.map(_.score).sum)
        JsObject(
          "name" -> JsString(name),
          "value" -> JsNumber(nodes.map(_.score).sum.toInt)
        )
    }.toVector

    val links = relations.map(
      relation =>
        JsObject(
          "source" -> JsString(relation.source),
          "target" -> JsString(relation.target),
          "score" -> JsNumber(relation.score.toInt)
        )
    ).toVector

    JsObject("data" -> JsArray(nodes),
      "links" -> JsArray(links))
  }
}
