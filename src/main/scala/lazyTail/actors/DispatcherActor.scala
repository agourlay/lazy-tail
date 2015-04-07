package lazyTail.actors

import akka.actor.{ Actor, ActorRef, Props }
import lazyTail.{ LazyLog, LogLevel }

class DispatcherActor extends Actor {
  import DispatcherActorProtocol._

  // TODO why not more stats: frequencies?
  val errors = scala.collection.mutable.Queue.empty[LazyLog]

  override def receive: Receive = {
    case log: LazyLog ⇒
      context.children.foreach { _ ! log }
      if (log.level == LogLevel.ERROR) {
        if (errors.size > 100) errors.dequeue()
        errors.enqueue(log)
      }
    case Subscribe ⇒
      sender() ! LogPublisherRef(context.actorOf(LogPublisher.props()))
    case AskLastErrors ⇒
      sender() ! LastErrors(errors.toVector)
  }
}

object DispatcherActor {
  def props() = Props(classOf[DispatcherActor])
}

object DispatcherActorProtocol {
  case object Subscribe
  case object AskLastErrors
  case class LastErrors(lastErrors: Vector[LazyLog])
  case class LogPublisherRef(ref: ActorRef)
}