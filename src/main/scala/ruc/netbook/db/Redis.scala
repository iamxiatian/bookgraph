package nlp.db

import akka.Done
import akka.actor.CoordinatedShutdown
import nlp.MyConf
import redis.RedisClient

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * Redis接口特质，方便混入到需要访问Redis的对象中
  *
  * @author Tian Xia
  *         May 09, 2017 00:21
  */
trait Redis {

  // error recovery functions to minimize copy/paste
  type Recovery[T] = PartialFunction[Throwable, T]

  // recover with None
  def withNone[T]: Recovery[Option[T]] = {
    case NonFatal(e) => None
  }

  // recover with empty sequence
  def withEmptySeq[T]: Recovery[Seq[T]] = {
    case NonFatal(e) => Seq()
  }


  implicit val akkaSystem = akka.actor.ActorSystem("RedisSystem")

  val log = akkaSystem.log

  val timeout = 5 seconds

  val redis = RedisClient(
    MyConf.getString("db.redis.host"),
    MyConf.getInt("db.redis.port"),
    connectTimeout = Some(6 seconds)
  )

  /**
    * 关闭Akka System的处理，外部只需要调用shutdown()接口
    */
  def shutdown(msg: String = "RedisClient closed."): Future[Done] = {
    val coordinatedShutdown = CoordinatedShutdown(akkaSystem)
    coordinatedShutdown.addJvmShutdownHook {
      println(msg)
    }
    coordinatedShutdown.run()
  }
}
