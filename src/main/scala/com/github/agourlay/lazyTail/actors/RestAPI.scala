package com.github.agourlay.lazyTail.actors

import akka.actor.{ Actor, ActorSystem, Props }
import akka.http.scaladsl.{ Http, server }
import akka.stream.ActorMaterializer

class RestAPI(logPort: Int, route: server.Route, system: ActorSystem, fm: ActorMaterializer) extends Actor {
  implicit val executionContext = system.dispatcher
  implicit val ifm = fm

  override def receive: Receive = Actor.emptyBehavior

  Http(system).bindAndHandle(route, "localhost", port = logPort)
}

object RestAPI {
  def props(port: Int, route: server.Route)(implicit system: ActorSystem, fm: ActorMaterializer) =
    Props(classOf[RestAPI], port, route, system, fm)
}