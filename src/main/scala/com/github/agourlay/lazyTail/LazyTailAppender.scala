package com.github.agourlay.lazyTail

import akka.actor.ActorRef
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory

case class LazyTailAppender(loggerName: String, dispatcherActor: ActorRef) {

  val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  val publisherAppender = new AppenderBase[ILoggingEvent]() {
    override def append(e: ILoggingEvent): Unit = dispatcherActor ! LazyLog.fromLogback(e)
  }
  publisherAppender.setContext(lc)
  publisherAppender.start()
  lc.getLogger(loggerName).addAppender(publisherAppender)
}
