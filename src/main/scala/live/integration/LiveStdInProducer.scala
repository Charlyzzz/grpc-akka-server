package live.integration

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import live.{EventRequest, Live, LiveClient}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Failure

object LiveStdInProducer {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem = ActorSystem("LiveStdInProducer", ConfigFactory.empty)
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = sys.dispatcher

    val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 9900).withTls(false)
    val client: Live = LiveClient(clientSettings)

    val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)

    stdinSource
      .map(_.utf8String)
      .filter(_.trim.nonEmpty)
      .runForeach(input => client.emitEvent(EventRequest(input, "topic")))
      .onComplete {
        case Failure(exception) => println(exception)
        case _ => println("Done!")
      }
  }
}
