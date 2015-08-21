package com.github.agourlay.lazyTail

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

class IntegrationSpec extends WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {

  val logger = LoggerFactory.getLogger("test-logger")

  override def afterAll() {

  }

  "LazyTail" must {
    "start service" in {
      LazyTail().start(9000)
      logger.info("log 1")
      logger.info("log 2")
      logger.info("log 3")
      logger.info("log 4")
      // TODO http request to read and assert stream
      // use akka-http client with ServerSentEventParser
    }
  }
}
