package lazyTail.actors

import akka.actor.{ Actor, ActorSystem, Props }
import akka.http.{ Http, server }
import akka.stream.ActorFlowMaterializer

class RestAPI(logPort: Int, route: server.Route, system: ActorSystem, fm: ActorFlowMaterializer) extends Actor {
  implicit val executionContext = system.dispatcher
  implicit val ifm = fm

  override def receive: Receive = Actor.emptyBehavior

  Http(system).bindAndHandle(route, "localhost", port = logPort)
}

object RestAPI {
  def props(port: Int, route: server.Route)(implicit system: ActorSystem, fm: ActorFlowMaterializer) =
    Props(classOf[RestAPI], port, route, system, fm)
}