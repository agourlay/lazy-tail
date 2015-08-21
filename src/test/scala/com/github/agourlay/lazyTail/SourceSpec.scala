package com.github.agourlay.lazyTail

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.slf4j.LoggerFactory

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

      val process = LazyTail().source(LogLevel.INFO).runWith(Sink.head)

      logger.info("catch me if you can")

      whenReady(process) { log ⇒
        log.formattedMessage should equal("catch me if you can")
      }

    }
  }
}
