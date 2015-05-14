package com.github.agourlay.lazyTail

import java.text.SimpleDateFormat
import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import ch.qos.logback.classic.spi.{ IThrowableProxy, ILoggingEvent }
import de.heikoseeberger.akkasse.ServerSentEvent
import spray.json._

case class LazyLog(
  threadName: String,
  level: LogLevel.LogLevelType,
  message: String,
  formattedMessage: String,
  loggerName: String,
  timestamp: Long,
  htmlLog: String)

object LazyLog extends JsonSupport {

  val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def fromLogback(e: ILoggingEvent) = {
    val formattedDate = sdf.format(new Date(e.getTimeStamp))
    val baseDisplay = s"$formattedDate <span class='log-${e.getLevel}'> ${e.getLevel} </span> ${e.getLoggerName} ${e.getFormattedMessage}"
    val exceptionOpts = if (e.getThrowableProxy == null) None else Some(ExceptionInfo.fromLogback(e.getThrowableProxy))
    val logDisplay = exceptionOpts.fold(baseDisplay) { eInfo ⇒
      baseDisplay + " </br> " + displayFullStacktrace(eInfo)
    }

    LazyLog(
      threadName = e.getThreadName,
      level = LogLevel.from(e.getLevel.toString).getOrElse(LogLevel.INFO), // need a log or something
      message = e.getMessage,
      formattedMessage = e.getFormattedMessage,
      loggerName = e.getLoggerName,
      timestamp = e.getTimeStamp,
      htmlLog = logDisplay
    )
  }

  private def displayFullStacktrace(eInfo: ExceptionInfo): String = {
    val causes = eInfo.accumulateCauses()
    displaySingleException(eInfo: ExceptionInfo) + "</br>Caused by: " + causes.map(displaySingleException).mkString("</br>Caused by: ")
  }

  private def displaySingleException(eInfo: ExceptionInfo): String = {
    s"${eInfo.className} ${eInfo.message} </br> &nbsp&nbsp " + eInfo.stacktrace.mkString(" </br> &nbsp&nbsp ")
  }

  private val toJson = implicitly[RootJsonFormat[LazyLog]]

  implicit def flowEventToSseMessage(log: LazyLog): ServerSentEvent = {
    ServerSentEvent(PrettyPrinter(toJson.write(log)), "log")
  }
}

case class ExceptionInfo(
    message: String,
    className: String,
    stacktrace: Vector[String],
    cause: Option[ExceptionInfo] = None) {

  def accumulateCauses(): Vector[ExceptionInfo] = {
    def loopAccumulateCauses(eInfo: ExceptionInfo, causes: Vector[ExceptionInfo]): Vector[ExceptionInfo] = {
      eInfo.cause.fold(causes) { cause ⇒ loopAccumulateCauses(cause, causes :+ cause) }
    }
    loopAccumulateCauses(this, Vector.empty[ExceptionInfo])
  }
}

object ExceptionInfo {
  def fromLogback(proxy: IThrowableProxy): ExceptionInfo =
    ExceptionInfo(
      message = proxy.getMessage,
      className = proxy.getClassName,
      stacktrace = proxy.getStackTraceElementProxyArray.toVector.map(_.getSTEAsString),
      cause = if (proxy.getCause == null) None else Some(fromLogback(proxy.getCause))
    )
}

object LogLevel extends Enumeration {
  type LogLevelType = Value

  val TRACE = Value("TRACE")
  val DEBUG = Value("DEBUG")
  val INFO = Value("INFO")
  val WARN = Value("WARN")
  val ERROR = Value("ERROR")

  def from(s: String): Option[Value] = values.find(_.toString == s)
}

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val formatLogLevel = new RootJsonFormat[LogLevel.LogLevelType] {
    def write(obj: LogLevel.LogLevelType) = JsString(obj.toString)
    def read(json: JsValue): LogLevel.LogLevelType = LogLevel.withName(json.prettyPrint)
  }
  implicit val formatLog = jsonFormat7(LazyLog.apply)
}