package lazyTail

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern._
import akka.http.server
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.Timeout
import lazyTail.actors.{ RestAPI, DispatcherActor, DispatcherActorProtocol }
import DispatcherActorProtocol.LogPublisherRef
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
 * LazyTail
 *
 * @param loggerName name of the logger to stream
 */
case class LazyTail(loggerName: String = "ROOT") {

  private def logSource(dispatcherActor: ActorRef)(implicit system: ActorSystem): Future[Source[LazyLog, Unit]] = {
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    implicit val ec = system.dispatcher

    (dispatcherActor ? DispatcherActorProtocol.Subscribe).mapTo[LogPublisherRef].map { logPublisher ⇒
      Source(ActorPublisher[LazyLog](logPublisher.ref))
    }
  }

  /**
   * Start lazyTail on provided port.
   * @param port http port
   */
  def start(port: Int): Unit = {
    implicit lazy val system = ActorSystem("lazy-tail")
    implicit lazy val mat = ActorFlowMaterializer()
    sys.addShutdownHook(system.shutdown())

    LoggerFactory.getLogger("LazyTail").info(s"Starting lazyTail on port $port")

    val dispatcherActor = system.actorOf(DispatcherActor.props())
    new LazyTailAppender(loggerName, dispatcherActor)
    val route = new LogRoute(logSource(dispatcherActor), dispatcherActor).build()

    system.actorOf(RestAPI.props(port, route))
  }

  /**
   * Build an Akka-Http Route with the lazyTail logic.
   * @param system akka-system running the Route
   * @return the Route
   */
  def route()(implicit system: ActorSystem): server.Route = {
    val dispatcherActor = system.actorOf(DispatcherActor.props())
    new LazyTailAppender(loggerName, dispatcherActor)
    new LogRoute(logSource(dispatcherActor), dispatcherActor).build()
  }

  /**
   * Build a Future of Source[Log, Unit] publishing logs.
   * @param system akka-system running the Source
   * @return Future of Source[Log, Unit]
   */
  def source()(implicit system: ActorSystem): Future[Source[LazyLog, Unit]] = {
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    implicit val ec = system.dispatcher

    val dispatcherActor = system.actorOf(DispatcherActor.props())
    new LazyTailAppender(loggerName, dispatcherActor)

    (dispatcherActor ? DispatcherActorProtocol.Subscribe).mapTo[LogPublisherRef].map { logPublisher ⇒
      Source(ActorPublisher[LazyLog](logPublisher.ref))
    }
  }
}

// IDEAS BACKEND
// - retrieve/specify log pattern
// - websocket support (wait 4 akka-http)

// IDEAS FRONTEND
// - min log level should be a SELECT box.