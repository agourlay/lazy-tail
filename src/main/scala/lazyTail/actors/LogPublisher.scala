package lazyTail.actors

import akka.actor.Props
import de.heikoseeberger.akkasse._
import lazyTail.{ JsonSupport, LogLevel, LazyLog }
import scala.concurrent.duration.DurationInt

class LogPublisher(minLogLevel: LogLevel.LogLevelType) extends EventPublisher[LazyLog](500, 1 second) with JsonSupport {

  override protected def receiveEvent = {
    case log: LazyLog ⇒
      if (log.level >= minLogLevel) {
        onEvent(log)
      }
  }
}

object LogPublisher {
  def props(minLogLevel: LogLevel.LogLevelType) = Props(classOf[LogPublisher], minLogLevel)
}
