package live.integration

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import live.{Live, LiveClient, SubRequest}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object LiveConsumer extends App {

  implicit val sys: ActorSystem = ActorSystem("LiveConsumer", ConfigFactory.empty())
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = sys.dispatcher

  val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 9900).withTls(false)
  val client: Live = LiveClient(clientSettings)

  val respuestas = client.subscribe(SubRequest("topic"))

  val done = respuestas.runFold(0)((numero, evento) => {
    println(s"Msg #$numero: ${evento.message}")
    numero + 1
  })

  done.onComplete {
    case Success(_) =>
      println("stream finalizado")
    case Failure(e) =>
      println(s"Error: $e")
  }
}
