package live.interfaces

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.Materializer
import live.Subscriber.Converter
import live._

import scala.concurrent.Future

case class Grpc(port: Int, interface: String = Interfaces.everywhere) extends Interface {

  implicit val grpcConverter: Converter[Event] = Event.apply

  override val name: String = "GRPC"

  override def up(implicit system: ActorSystem, materializer: Materializer): Future[Http.ServerBinding] = {
    val service: HttpRequest => Future[HttpResponse] =
      LiveHandler(new LiveImpl)

    val handler: HttpRequest => Future[HttpResponse] = { request =>
      val withoutEncoding = request.copy(headers = request.headers.filterNot(_.name == "grpc-accept-encoding"))
      service(withoutEncoding)
    }

    Http().bindAndHandleAsync(
      handler,
      interface = interface,
      port = port,
      connectionContext = HttpConnectionContext())
  }
}
