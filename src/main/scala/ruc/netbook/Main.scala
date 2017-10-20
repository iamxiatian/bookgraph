package nlp

import java.io.File

import akka.actor.{ActorSystem, PoisonPill, Props}
import nlp.api.HttpApiServer
import nlp.api.actor.ScheduleActor
import nlp.opinion.EntityGraph
import org.zhinang.util.FileUtils
import org.zhinang.util.verhas.{HardwareBinder, PGPSafeService, SafeService}

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


  //如果本地有私钥，则直接生成许可，避免因网卡IP地址变化，导致许可失败
  val secringFile = ".cache.tmp" //".secring.gpg"
  if (new File(secringFile).exists()) {
    val content: String = PGPSafeService.generate(new HardwareBinder().getMachineIdString,
      "2019-12-31",
      "zhinang",
      secringFile)
    FileUtils.saveToFile(new File("license.dat"), content)
  }

  if (SafeService.check()) {
    println("Congratulation, passed. :-)")
  } else {
    println(s"Your machine id: ${new HardwareBinder().getMachineIdString}")
    println("Something was wrong, :(")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      if (config.license) {
        println(s"Your machine id: ${new HardwareBinder().getMachineIdString}")
        if (SafeService.check())
          println("Congratulation, passed. :-)")
        else
          println("License is not valid, :(")
      }

      if (config.server) {
        startServer(NlpConf.httpPort)
      }

      if (config.createEntityGraph) {
        EntityGraph.createGraphFromFile()
        println("Finished.")
      }
    case None => {
      println("Wrong parameters :(")
    }
  }

  def startServer(port: Int): Unit = {
    val system = ActorSystem("nlpSystem")
    implicit val executionContext = system.dispatcher

    val scheduler = system.actorOf(Props(classOf[ScheduleActor], system), "scheduler")
    println("Starting scheduler actor...")
    scheduler ! "Start"

    val bindingFuture = HttpApiServer.start(system, port)
    println(s"Server online at http://localhost:$port")

    sys.addShutdownHook {
      println("shutdown server...")

      println("Shutting down scheduler...")
      scheduler ! PoisonPill

      //当Rest服务关闭时，同时把system停止
      implicit val executionContext = system.dispatcher
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

      //不用再调用terminate，因为在解除Rest服务端口绑定时，已经调用terminate
      //system.terminate

      println("server closed.")
    }
  }

}
