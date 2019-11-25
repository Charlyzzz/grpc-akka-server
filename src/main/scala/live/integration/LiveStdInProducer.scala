package live.integration

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import com.typesafe.config.ConfigFactory
import live.{EventRequest, Live, LiveClient}

import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure

object LiveStdInProducer extends App {

  implicit val sys: ActorSystem = ActorSystem("ConstantRateProducer", ConfigFactory.empty)
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = sys.dispatcher

  val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 9900).withTls(false)
  val client: Live = LiveClient(clientSettings)

  val stdinSource = StreamConverters.fromInputStream(() => System.in)

  stdinSource
    .map(_.utf8String)
    .filter(_.trim.nonEmpty)
    .runForeach(input => client.emitEvent(EventRequest(input, "example")))
    .onComplete {
      case Failure(exception) => println(exception)
      case _ => println("Done!")
    }
}
