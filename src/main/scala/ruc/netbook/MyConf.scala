package nlp

import com.typesafe.config.ConfigFactory

/**
  * NLP项目的配置
  *
  * @author Tian Xia
  *         Apr 20, 2017 1:31 AM
  */
object MyConf {
  //先采用my.conf中的配置，再使用application.conf中的默认配置
  lazy val config = ConfigFactory.parseFile(new java.io.File("conf/my.conf"))
    .withFallback(ConfigFactory.load())

  def getString(key: String) = config.getString(key)

  def getInt(key: String) = config.getInt(key)

  def getBoolean(path: String) = config.getBoolean(path)

  lazy val httpPort = getInt("http.port")
  lazy val mongoUrl = getString("db.mongo.url")


  /**
    * 返回触发的时间点（每一个时间点都是一对整数： (小时, 分钟)）
    */
  lazy val triggerTimes = getString("scheduler.triggerTimes").split(" ")
    .map {
      t =>
        val parts = t.split(":").map(_ toInt)
        (parts(0), parts(1))
    }.toList

  lazy val mailNotify = getBoolean("scheduler.mail.notify") //是否启用邮件通知

  def printConf() = {
    println(s"http server port ==> $httpPort")

    println("  scheduler config:")
    println(s"\t triggerTimes ==> $triggerTimes")
    println(s"\t mail.notify ==> $mailNotify")
    if (mailNotify) {
      println(s"\t mail.smtp.host ==>" +
        s" ${getString("scheduler.mail.smtp.host")}")
      println(s"\t mail.smtp.port ==>" +
        s" ${getInt("scheduler.mail.smtp.port")}")
      println(s"\t mail.smtp.user ==>" +
        s" ${getString("scheduler.mail.smtp.user")}")
      println(s"\t mail.smtp.auth ==>" +
        s" ${getBoolean("scheduler.mail.smtp.auth")}")
      println(s"\t mail.smtp.startTtls ==>" +
        s" ${getBoolean("scheduler.mail.smtp.startTtls")}")
      println(s"\t mail.receivers ==>" +
        s" ${getString("scheduler.mail.receivers")}")
    }
  }
}
