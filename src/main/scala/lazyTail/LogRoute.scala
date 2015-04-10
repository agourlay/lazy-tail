package lazyTail

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.marshalling.ToResponseMarshallable
import akka.http.model.{ MediaTypes, HttpEntity, HttpResponse }
import akka.pattern._
import akka.http.model.StatusCodes._
import akka.stream.scaladsl.Source
import akka.http.server._
import akka.http.model.headers._
import akka.http.model.headers.CacheDirectives._
import akka.util.Timeout

import de.heikoseeberger.akkasse.{ ServerSentEvent, EventStreamMarshalling }
import lazyTail.actors.DispatcherActorProtocol
import DispatcherActorProtocol.{ LastErrors, AskLastErrors }
import spray.json.PrettyPrinter

import scala.concurrent.Future

class LogRoute(sourceOfLogs: LogLevel.LogLevelType ⇒ Future[Source[LazyLog, Unit]], dispatcherActor: ActorRef)
    extends EventStreamMarshalling with Directives with JsonSupport {

  implicit def flowEventToSseMessage(log: LazyLog): ServerSentEvent = {
    ServerSentEvent(eventType = "log", data = PrettyPrinter(formatLog.write(log)))
  }

  def build()(implicit system: ActorSystem) = {
    implicit val ec = system.dispatcher
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    val maxAge = 60L * 60L * 24L * 31L

    pathPrefix("logs") {
      get {
        path("tail") {
          //TODO serialize directly as enum as in Spray
          parameters('minLevel ? "INFO") { param: String ⇒
            complete {
              LogLevel.from(param.toUpperCase).fold(ToResponseMarshallable(BadRequest -> s"$param is not a valid LogLevel")) { minLogLevel ⇒
                // TODO why is the implicit not picked up?
                sourceOfLogs(minLogLevel).map(_.map(flowEventToSseMessage))
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
                  entity = HttpEntity(MediaTypes.`text/html`,
                    "<html>" +
                      "<head>" +
                      "<link rel='stylesheet' href='/logs/css/pure-min.css'/>" +
                      "<link rel='stylesheet' href='/logs/css/logs.css'/>" +
                      "</head>" +
                      "<body>" +
                      "<div class='main-content'>" +
                      container.lastErrors.map(_.htmlLog).mkString("</br>") +
                      "</div>" +
                      "</body>" +
                      "</html>"))
              )
            }
          } ~
            path("lastErrors") {
              onSuccess((dispatcherActor ? AskLastErrors).mapTo[LastErrors]) { container: LastErrors ⇒
                complete(ToResponseMarshallable(OK -> container.lastErrors))
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
}
