package live.integration

import akka.actor.{ActorSystem, Scheduler}
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{TextMessage, WebSocketRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import live.{Live, LiveClient}

import scala.concurrent.{Await, ExecutionContext, Future}

class GrpcClient(clientAddress: String = "localhost", clientPort: Int = 9900) extends App {

  val port = clientPort
  val address = clientAddress

  implicit val system: ActorSystem = ActorSystem("LiveClient", ConfigFactory.empty)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val clientSettings = GrpcClientSettings.connectToServiceAt(address, port).withTls(false)
  val client: Live = LiveClient(clientSettings)
}

class WsClient extends App {

  implicit val system: ActorSystem = ActorSystem("LiveClient", ConfigFactory.empty)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
}

object ConstantRateProducer extends WsClient {

  import akka.pattern.after

  import scala.concurrent.duration._

  implicit val scheduler: Scheduler = system.scheduler

  def delayed[T](value: T, duration: FiniteDuration): Future[T] = after(duration, scheduler)(Future(value))

  val wsFlow = Http().webSocketClientFlow(WebSocketRequest(s"ws://localhost:9900/ws/example"))

  private val blitzkriegBop: List[(String, FiniteDuration)] = List(
    ("hey", 2.second),
    ("ho", 1.second),
    ("let's", 1700.milliseconds),
    ("go", 300.milliseconds)
  )

  Source.unfoldAsync(blitzkriegBop) {
    case (head@(word, delay)) :: rest =>
      Future(Some(rest :+ head, delayed(word, delay)))
  }
    .map(Await.result(_, 5.seconds))
    .map(TextMessage(_))
    .via(wsFlow)
    .runWith(Sink.ignore)
    .onComplete {
      case msg => println(msg)
    }

}

