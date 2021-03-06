package com.github.agourlay.lazyTail

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server._
import akka.pattern._
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.github.agourlay.lazyTail.actors.DispatcherActorProtocol.{ AskLastErrors, LastErrors }
import de.heikoseeberger.akkasse.{ ServerSentEvent, EventStreamMarshalling }
import scala.concurrent.duration._

case class LogRoute(sourceOfLogs: LogLevel.LogLevelType ⇒ Source[LazyLog, NotUsed], dispatcherActor: ActorRef)(implicit system: ActorSystem)
    extends Directives with EventStreamMarshalling with JsonSupport {

  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val maxAge = 60L * 60L * 24L * 31L

  val route = pathPrefix("logs") {
    get {
      path("tail") {
        //TODO serialize directly as enum as in Spray
        parameters('minLevel ? "INFO") { param: String ⇒
          complete {
            LogLevel.from(param.toUpperCase).fold(ToResponseMarshallable(BadRequest → s"$param is not a valid LogLevel")) {
              sourceOfLogs(_)
                .map(LazyLog.flowEventToSseMessage)
                .keepAlive(1.second, () ⇒ ServerSentEvent.heartbeat)
            }
          }
        }
      }
    } ~
      get {
        path("lastErrors.html") {
          onSuccess((dispatcherActor ? AskLastErrors).mapTo[LastErrors]) { container: LastErrors ⇒
            complete(
              HttpResponse(
                //TODO so lazy...try Twirl or Scalate
                entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  s"""
                      <html>
                      <head>
                      <link rel='stylesheet' href='/logs/css/pure-min.css'/>
                      <link rel='stylesheet' href='/logs/css/logs.css'/>
                      </head>
                      <body>
                      <div class='main-content'>
                      ${container.lastErrors.map(_.htmlLog).mkString("</br>")}
                      </div>
                      </body>
                      </html>
                     """
                )
              )
            )
          }
        } ~
          path("lastErrors") {
            onSuccess((dispatcherActor ? AskLastErrors).mapTo[LastErrors]) { container: LastErrors ⇒
              complete(ToResponseMarshallable(OK → container.lastErrors))
            }
          }
      } ~
      pathEndOrSingleSlash {
        respondWithHeader(`Cache-Control`(`public`, `max-age`(maxAge))) {
          getFromResource("frontend/logs.html")
        }
      } ~
      respondWithHeader(`Cache-Control`(`public`, `max-age`(maxAge))) {
        getFromResourceDirectory("frontend")
      }
  }
}
