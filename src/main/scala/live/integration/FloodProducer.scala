package live.integration

import akka.actor.{ActorSystem, Scheduler}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import live.{EventRequest, Live, LiveClient}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

object FloodProducer extends App {

  implicit val system: ActorSystem = ActorSystem("LiveClient", ConfigFactory.empty)
  implicit val scheduler: Scheduler = system.scheduler
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  def delayed[T](value: T, duration: FiniteDuration): Future[T] = akka.pattern.after(duration, scheduler)(Future(value))

  val address = "a7445122ec9f5453f89b9ef8a4ee630e-658052086.us-east-1.elb.amazonaws.com"
  val port = 80
  private val clientSettings = GrpcClientSettings.connectToServiceAt(address, port).withTls(false)




  0.to(4).foreach { _ =>
    val client: Live = LiveClient(clientSettings)

    Source.repeat("Hey apple!")
      .throttle(100, 1.second)
      .map(EventRequest(_, "example"))
      .runForeach {
        client.emitEvent(_).onComplete {
          case Failure(e) =>
            println(e)
          case _ =>
        }
      }
  }
}

