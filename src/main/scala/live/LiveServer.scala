package live

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContext, Future}

object LiveServer {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("live-cluster")
    new LiveServer(system).run()
  }
}

class LiveServer(system: ActorSystem) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem = system
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
    Cluster(system).registerOnMemberUp(system.log.info("Cluster is up!"))

    val service: HttpRequest => Future[HttpResponse] =
      LiveHandler(new LiveImpl)

    val handler: HttpRequest => Future[HttpResponse] = { request =>
      val withoutEncoding = request.copy(headers = request.headers.filterNot(_.name == "grpc-accept-encoding"))
      service(withoutEncoding)
    }

    // Bind service handler servers to localhost:8080/8081
    val binding = Http().bindAndHandleAsync(
      handler,
      interface = "0.0.0.0",
      port = scala.sys.env("GRPC_PORT").toInt,
      connectionContext = HttpConnectionContext())

    // report successful binding
    binding.foreach { binding =>
      println(s"gRPC server bound to: ${binding.localAddress}")
    }

    binding
  }
}