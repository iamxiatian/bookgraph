package nlp.api.actor

import java.util.{Calendar, Date}

import akka.actor.{Actor, ActorLogging, ActorSystem}
import nlp.NlpConf
import nlp.data.DataReport
import nlp.util.SmtpMailer
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success


/**
  * Master节点上执行的调度程序，目前的调度任务包括：
  *
  * <ul>
  * <li>根据triggerTimes中指定的时间点，汇报今天当前抓取的结果</li>
  * </ul>
  *
  * @author Tian Xia
  *         May 15, 2017 22:09
  */
class ScheduleActor(system: ActorSystem) extends Actor with ActorLogging {
  val triggerTimes = NlpConf.triggerTimes

  import ScheduleActor._

  def trySchedule(runJobNow: Boolean) = {
    val interval = calculateDelay(triggerTimes)
    if (interval.isEmpty) {
      log.warning("SCHEDULER TRIGGER TIMES is not SET" +
        "(scheduler.triggerTimes).")
      system.scheduler.scheduleOnce(10 minutes, self, ScheduleJob)
    } else {
      if (runJobNow) {
        self ! RunJob
      }
      println("time:" + (interval.get + 120))
      //比计算出的延迟时间再延迟2分钟
      system.scheduler.scheduleOnce(
        (interval.get + 120) seconds,
        self,
        ScheduleJob
      )
    }
  }

  def receive: PartialFunction[Any, Unit] = {
    case "Start" =>
      if (NlpConf.mailNotify) trySchedule(false)

    case RunJob =>
      log.info(s"RUN JOB at ${DateTime.now.toString("yyyy-MM-dd HH:mm:ss")}")
      sendMail()

    case ScheduleJob =>
      trySchedule(true)
  }

  def sendMail() = {
    println("==============================")
    println(s"Send Mail at ${DateTime.now.toString("yyyy-MM-dd HH:mm:ss")}")
    println("==============================")

    val format: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    //汇报8月22日到今天的每天结果
    val startDate = DateTime.parse("2017-08-22", format).toDate

    val endDates: List[Date] = (1 to 100)
      .map(i => new Date(startDate.getTime + 86400000L * i))
      .toList
      .filter(_.getTime < System.currentTimeMillis() + 86400000L)
      .reverse

    Future.sequence(
      endDates.map(
        d =>
          DataReport.siteDistributionOfArticles(Some(startDate), Some(d))
            .flatMap {
              lst =>
                val fromTip = new DateTime(startDate).toString("yyyy-MM-dd")
                val endTip = new DateTime(d).toString("yyyy-MM-dd")
                val title = s"世界机器人大会统计结果(${fromTip}至${endTip})"
                val body = lst.map(pair => s"${pair._1}\t${pair._2}")
                  .mkString("\n")
                Future.successful((title, body))
            }
      ))
      .flatMap {
        dayItems =>
          Future.successful(
            dayItems.map {
              case (title: String, body: String) =>
                s"$title\n$body"
            }.mkString("\n\n---------------\n\n")
          )
      }.onComplete {
      case Success(body: String) =>
        val subject = s"${DateTime.now.toString("yyyy-MM-dd HH:mm")}舆情统计汇报结果"
        SmtpMailer.sendTextMail(subject, body)
      case _ => log.error("Error when make mail body.")
    }
  }
}

/**
  * Created by deepak on 22/1/17.
  */
object ScheduleActor {

  case object ScheduleJob

  case object RunJob

  def timeToSeconds(hour: Int, minute: Int) = {
    val c = Calendar.getInstance
    c.set(Calendar.HOUR_OF_DAY, hour)
    c.set(Calendar.MINUTE, minute)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.getTimeInMillis / 1000
  }

  /**
    * 根据triggerTimes中提供的触发时间点列表，找出最近一次应该触发调度处理的
    * 时间点，并计算和当前时间的间隔，作为任务延迟调度的时间量
    *
    * @param triggerTimes like: List("8:00", "10:00", "12:00", "14:00", "17:00")
    * @return 距离下次被调度的秒数，如没有满足条件的结果，则返回None
    */
  def calculateDelay(triggerTimes: List[(Int, Int)]): Option[Long] = {
    val nowInSeconds = System.currentTimeMillis() / 1000

    //从所有候选的triggerTimes中，寻找当满足如下条件的第一个元素，该元素
    //大于等于当前时间，差值作为下次被调度的时间
    val triggerDelay: Option[Long] = triggerTimes
      .map(t => timeToSeconds(t._1, t._2) - nowInSeconds)
      .find(_ >= 0)

    if (triggerDelay.isEmpty) {
      //没有满足条件的下一次触发时间，则说明当时所有调度都已经执行成功
      //应该进入下一天的首次调度
      val firstTrigger = triggerTimes.headOption
      if (firstTrigger.isEmpty) {
        None
      } else {
        // 下一天的首次触发时间减去当前时间
        Some(
          timeToSeconds(firstTrigger.get._1, firstTrigger.get._2)
            + 86400 - nowInSeconds)
      }
    } else
      triggerDelay
  }
}

