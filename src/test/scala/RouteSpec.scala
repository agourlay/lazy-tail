import akka.actor.ActorSystem
import akka.testkit.TestKit
import lazyTail.LazyTail

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

class RouteSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  val logger = LoggerFactory.getLogger("test-logger")

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "Route API" must {
    "return Route" in {
      LazyTail().route()
    }
  }
}
