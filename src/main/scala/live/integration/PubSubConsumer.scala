package live.integration

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import live.{PubSub, PubSubClient, SubRequest}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object PubSubConsumer {

  def main(args: Array[String]): Unit = {

    implicit val sys: ActorSystem = ActorSystem("PubSubConsumer", ConfigFactory.empty())
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = sys.dispatcher

    val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 9090)
    val client: PubSub = PubSubClient(clientSettings)

    val respuestas = client.subscribe(SubRequest("topic"))

    val x = respuestas.runFold(0)((numero, evento) => {
      println(s"Msg #$numero: ${evento.message}")
      numero + 1
    })

    x.onComplete {
      case Success(_) =>
        println("stream finalizado")
      case Failure(e) =>
        println(s"Error: $e")
    }
  }
}
