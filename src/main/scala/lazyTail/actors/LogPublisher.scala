package lazyTail.actors

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import lazyTail.{ LogLevel, LazyLog }

class LogPublisher(minLogLevel: LogLevel.LogLevelType) extends ActorPublisher[LazyLog] {

  val logs = scala.collection.mutable.Queue.empty[LazyLog]

  override def receive: Receive = {
    case log: LazyLog ⇒
      if (log.level >= minLogLevel) {
        // drop old logs if Queue gets too big
        if (logs.size > 500) logs.dequeue()
        logs.enqueue(log)
        pushToSub()
      }

    case Request(_) ⇒
      pushToSub()

    case Cancel ⇒
      context.stop(self)
  }

  def pushToSub() {
    while (totalDemand > 0 && logs.nonEmpty) {
      onNext(logs.dequeue())
    }
  }
}

object LogPublisher {
  def props(minLogLevel: LogLevel.LogLevelType) = Props(classOf[LogPublisher], minLogLevel)
}