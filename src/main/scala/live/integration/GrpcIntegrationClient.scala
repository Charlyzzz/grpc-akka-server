package live.integration


import akka.actor.{ActorSystem, Scheduler}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import live.{Interfaces, Live, LiveClient}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class GrpcIntegrationClient(address: String = Interfaces.localhost, port: Int = 9900) extends App {

  implicit val system: ActorSystem = ActorSystem("LiveClient", ConfigFactory.empty)
  implicit val scheduler: Scheduler = system.scheduler
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  def delayed[T](value: T, duration: FiniteDuration): Future[T] = akka.pattern.after(duration, scheduler)(Future(value))

  private val clientSettings = GrpcClientSettings.connectToServiceAt(address, port).withTls(false)
  val client: Live = LiveClient(clientSettings)
}
