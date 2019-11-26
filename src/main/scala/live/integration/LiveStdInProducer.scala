package live.integration

import akka.stream.scaladsl.StreamConverters
import live.EventRequest

import scala.util.Failure

object LiveStdInProducer extends GrpcIntegrationClient {

  val stdinSource = StreamConverters.fromInputStream(() => System.in)

  stdinSource
      .map(_.utf8String)
      .filter(_.trim.nonEmpty)
      .runForeach(input => client.emitEvent(EventRequest(input, "example")))
      .onComplete {
        case Failure(exception) =>
          println(exception)
          system.terminate()
        case _ => println("Done!")
      }
}
