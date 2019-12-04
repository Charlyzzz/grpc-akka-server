package live.integration

import akka.stream.scaladsl.Source
import live.EventRequest

import scala.concurrent.duration._
import scala.util.Failure

object FloodProducer2 extends GrpcIntegrationClient(address = "a7445122ec9f5453f89b9ef8a4ee630e-658052086.us-east-1.elb.amazonaws.com", port = 80) {

  Source.repeat("Hey apple!")
    .throttle(50, 1.second)
    .map(EventRequest(_, "example"))
    .runForeach {
      client.emitEvent(_).onComplete {
        case Failure(e) =>
          println(e)
        case _ =>
      }
    }
}

