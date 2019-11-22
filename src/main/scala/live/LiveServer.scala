package live

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object LiveServer extends App {

  val env = sys.env.getOrElse("ENV", "dev")
  val config = ConfigFactory.load(env)
  implicit val system: ActorSystem = ActorSystem("live-cluster", config)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  val log = system.log

  Try(LiveServer.run) match {
    case Failure(exception) =>
      log.error(exception, "Shutting down due to error")
      system.terminate()
    case Success(_) =>
      log.info("Shutting down system")
  }

  private def run: Future[Http.ServerBinding] = {
    startClusterFormation()
    val grpcPort = sys.env.getOrElse("GRPC_PORT", "8080").toInt
    startGrpcServer(grpcPort)
  }

  private def startGrpcServer(port: Int): Future[Http.ServerBinding] = {
    val service: HttpRequest => Future[HttpResponse] =
      LiveHandler(new LiveImpl)

    val handler: HttpRequest => Future[HttpResponse] = { request =>
      val withoutEncoding = request.copy(headers = request.headers.filterNot(_.name == "grpc-accept-encoding"))
      service(withoutEncoding)
    }

    val binding = Http().bindAndHandleAsync(
      handler,
      interface = "0.0.0.0",
      port = port,
      connectionContext = HttpConnectionContext())

    binding.foreach { binding =>
      log.info(s"gRPC server bound to: ${binding.localAddress}")
    }
    binding
  }

  private def startClusterFormation(): Unit = {
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
    Cluster(system).registerOnMemberUp(system.log.info("Member is up!"))
  }
}