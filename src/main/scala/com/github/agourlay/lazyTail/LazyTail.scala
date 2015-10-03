package com.github.agourlay.lazyTail

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.server
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.github.agourlay.lazyTail.actors.DispatcherActorProtocol.LogPublisherRef
import com.github.agourlay.lazyTail.actors.{ DispatcherActor, DispatcherActorProtocol, RestAPI }
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * LazyTail
 *
 * @param loggerName name of the logger to stream
 */
case class LazyTail(loggerName: String = "ROOT") {

  private def logSource(dispatcherActor: ActorRef, minLogLevel: LogLevel.LogLevelType)(implicit system: ActorSystem): Source[LazyLog, Unit] = {
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    implicit val ec = system.dispatcher

    val f = (dispatcherActor ? DispatcherActorProtocol.Subscribe(minLogLevel)).mapTo[LogPublisherRef].map { logPublisher ⇒
      Source(ActorPublisher[LazyLog](logPublisher.ref))
    }
    Await.result(f, 5.seconds)
  }

  /**
   * Start lazyTail on provided port.
   * @param port http port
   */
  def start(port: Int): Unit = {
    implicit lazy val system = ActorSystem("lazy-tail")
    implicit lazy val mat = ActorMaterializer()
    sys.addShutdownHook(system.shutdown())

    LoggerFactory.getLogger("LazyTail").info(s"Starting lazyTail on port $port")

    val dispatcherActor = system.actorOf(DispatcherActor.props())
    new LazyTailAppender(loggerName, dispatcherActor)
    val logRoute = LogRoute(logSource(dispatcherActor, _), dispatcherActor)

    system.actorOf(RestAPI.props(port, logRoute.route))
  }

  /**
   * Build an Akka-Http Route with the lazyTail logic.
   * @param system akka-system running the Route
   * @return the Route
   */
  def route()(implicit system: ActorSystem): server.Route = {
    val dispatcherActor = system.actorOf(DispatcherActor.props())
    LazyTailAppender(loggerName, dispatcherActor)
    LogRoute(logSource(dispatcherActor, _), dispatcherActor).route
  }

  /**
   * Build a Future of Source[Log, Unit] publishing logs.
   * @param system akka-system running the Source
   * @return Source[LazyLog, Unit]
   */
  def source(minLogLevel: LogLevel.LogLevelType)(implicit system: ActorSystem): Source[LazyLog, Unit] = {
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    implicit val ec = system.dispatcher

    val dispatcherActor = system.actorOf(DispatcherActor.props())
    LazyTailAppender(loggerName, dispatcherActor)

    val f = (dispatcherActor ? DispatcherActorProtocol.Subscribe(minLogLevel)).mapTo[LogPublisherRef].map { logPublisher ⇒
      Source(ActorPublisher[LazyLog](logPublisher.ref))
    }
    Await.result(f, 5.seconds)
  }
}