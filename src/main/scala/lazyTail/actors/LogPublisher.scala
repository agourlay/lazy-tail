package lazyTail.actors

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import lazyTail.LazyLog

class LogPublisher extends ActorPublisher[LazyLog] {

  val logs = scala.collection.mutable.Queue.empty[LazyLog]

  override def receive: Receive = {
    case log: LazyLog ⇒
      // drop old logs if Queue gets too big
      if (logs.size > 500) logs.dequeue()
      logs.enqueue(log)
      pushToSub()

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
  def props() = Props(classOf[LogPublisher])
}