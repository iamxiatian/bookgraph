package ruc.bookgraph

import akka.actor.ActorSystem
import nlp.MyConf
import ruc.bookgraph.api.HttpApiServer

/**
  * 主程序，用于运行服务，显示结果
  *
  * @author Tian Xia
  *         Apr 24, 2017 7:55 PM
  */
object Main extends App {

  case class Config(
                     createEntityGraph: Boolean = false,
                     license: Boolean = false,
                     server: Boolean = false)

  val parser = new scopt.OptionParser[Config]("bin/nlp") {
    head("NLP Toolkit", "1.0")

    opt[Unit]("server").action((_, c) =>
      c.copy(server = true)).text("Start API Server.")

    opt[Unit]("create").action((_, c) =>
      c.copy(createEntityGraph = true)).text("Create entity graph.")

    opt[Unit]("license").action((_, c) =>
      c.copy(license = true)).text("License information.")

    help("help").text("prints this usage text")

    note("\n xiatian, xia@ruc.edu.cn.")
  }


  parser.parse(args, Config()) match {
    case Some(config) =>

      if (config.server) {
        startServer(MyConf.httpPort)
      }

    case None => {
      println("Wrong parameters :(")
    }
  }

  def startServer(port: Int): Unit = {
    val system = ActorSystem("nlpSystem")
    implicit val executionContext = system.dispatcher

    val bindingFuture = HttpApiServer.start(system, port)
    println(s"Server online at http://localhost:$port")

    sys.addShutdownHook {
      println("shutdown server...")

      //当Rest服务关闭时，同时把system停止
      implicit val executionContext = system.dispatcher
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

      //不用再调用terminate，因为在解除Rest服务端口绑定时，已经调用terminate
      //system.terminate

      println("server closed.")
    }
  }

}
