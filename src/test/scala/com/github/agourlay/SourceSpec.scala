package com.github.agourlay

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import com.github.agourlay.lazyTail.{ LazyTail, LogLevel }
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.slf4j.LoggerFactory
import spray.json.{ JsString, JsonParser }

class SourceSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with ScalaFutures with Eventually with BeforeAndAfterAll {
  def this() = this(ActorSystem("MySpec"))

  val logger = LoggerFactory.getLogger("test-logger")

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "Source API" must {
    "return Source" in {
      implicit lazy val mat = ActorMaterializer()
      implicit lazy val ec = system.dispatcher

      val futureSource = LazyTail().source(LogLevel.INFO)
      whenReady(futureSource) { s ⇒
        val process = s.runWith(Sink.head)

        logger.info("catch me if you can")

        whenReady(process) { log ⇒
          JsonParser(log.data).asJsObject.getFields("formattedMessage").head should equal(JsString("catch me if you can"))
        }
      }
    }
  }
}
