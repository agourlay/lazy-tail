import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.testkit.TestKit
import lazyTail.{ LogLevel, LazyTail }

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

class SourceSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {
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
        logger.info("catch me if you can")
        // FIXME
        s.runForeach { log ⇒ log.formattedMessage should be("catch me if you can") }
      }
    }
  }
}
