package nlp.util

import javax.mail.internet.InternetAddress

import nlp.NlpConf
import nlp.util.mail.Defaults._
import nlp.util.mail._
import org.slf4j.LoggerFactory

import scala.util.Success

/**
  * 根据配置文件中的邮件设置参数，发送邮件
  */
object SmtpMailer {
  val log = LoggerFactory.getLogger("SmtpMailer")

  val host = NlpConf.getString("scheduler.mail.smtp.host")
  val port = NlpConf.getInt("scheduler.mail.smtp.port")
  val user = NlpConf.getString("scheduler.mail.smtp.user")
  val password = NlpConf.getString("scheduler.mail.smtp.password")
  val auth = NlpConf.getBoolean("scheduler.mail.smtp.auth")
  val startTtls = NlpConf.getBoolean("scheduler.mail.smtp.startTtls")

  val receivers = NlpConf.getString("scheduler.mail.receivers").split(";")
    .filter(_.contains("@"))
    .map(new InternetAddress(_))
    .toList

  val mailer = Mailer(host, port)
    .auth(auth)
    .as(user, password)
    .startTtls(startTtls)()

  def sendHtmlMail(subject: String, htmlBody: String): Unit =
    mailer(
      Envelope.from(
        //"cnxiatian" `@` "163.com"
        new InternetAddress(user)
      )
        .to(
          //"cnxiatian" `@` "163.com"
          receivers: _*
        )
        .subject(subject)
        .content(Multipart()
          //.attach(new java.io.File("tps.xls"))
          .html(htmlBody)))
      .onComplete {
        case Success(_) =>
          log.info(s"Email about $subject has been deliverd.")
        case _ =>
          log.error(s"Error when sending email $subject")
      }


  def sendTextMail(subject: String, body: String): Unit =
    mailer(
      Envelope.from(
        //"cnxiatian" `@` "163.com"
        new InternetAddress(user)
      )
        .to(
          //"cnxiatian" `@` "163.com"
          receivers: _*
        )
        .subject(subject)
        .content(Multipart()
          //.attach(new java.io.File("tps.xls"))
          .text(body)))
      .onComplete {
        case Success(_) =>
          log.info(s"Email about $subject has been deliverd.")
        case _ =>
          log.error(s"Error when sending email $subject")
      }
}