import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import lazyTail.{ LazyLog, LogLevel, LazyTail }

import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Millis, Span }
import org.slf4j.LoggerFactory

class SourceSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with ScalaFutures with Eventually with BeforeAndAfterAll {
  def this() = this(ActorSystem("MySpec"))

  val logger = LoggerFactory.getLogger("test-logger")

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "Source API" must {
    "return Source" in {
      implicit lazy val mat = ActorFlowMaterializer()
      implicit lazy val ec = system.dispatcher

      val futureSource = LazyTail().source(LogLevel.INFO)
      whenReady(futureSource) { s ⇒
        val process = s.runWith(Sink.head)

        logger.info("catch me if you can")

        whenReady(process) { log ⇒
          log.formattedMessage should equal("catch me if you can")
        }
      }
    }
  }
}
