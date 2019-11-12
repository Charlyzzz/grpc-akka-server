package live.integration

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import live.{EventRequest, PubSub, PubSubClient}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Failure

object PubSubProducer {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.empty()
    implicit val sys: ActorSystem = ActorSystem("PubSubProducer", config)
    implicit val mat = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = sys.dispatcher

    val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 9090).withTls(false)
    val client: PubSub = PubSubClient(clientSettings)

    val stdinSource: Source[ByteString, Future[IOResult]] = StreamConverters.fromInputStream(() => System.in)

    stdinSource
        .runForeach(input => client.emitEvent(EventRequest(input.utf8String, "topic")))
        .onComplete { case Failure(exception) => println(exception) }

  }
}
