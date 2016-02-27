package com.github.agourlay.lazyTail.actors

import akka.actor.{ Actor, ActorRef, Props }
import com.github.agourlay.lazyTail.{ LazyLog, LogLevel }

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
    case Subscribe(minLogLevel) ⇒
      sender() ! LogPublisherRef(context.actorOf(LogPublisher.props(minLogLevel)))
    case AskLastErrors ⇒
      sender() ! LastErrors(errors.toVector)
  }
}

object DispatcherActor {
  def props() = Props(classOf[DispatcherActor])
}

object DispatcherActorProtocol {
  case class Subscribe(minLogLevel: LogLevel.LogLevelType)
  case object AskLastErrors
  case class LastErrors(lastErrors: Vector[LazyLog])
  case class LogPublisherRef(ref: ActorRef)
}