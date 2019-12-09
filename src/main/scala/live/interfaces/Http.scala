package live.interfaces

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import live.Subscriber.Converter
import live._

import scala.concurrent.Future

case class Http(port: Int, interface: String = Interfaces.everywhere) extends Interface {

  implicit val grpcConverter: Converter[Event] = Event.apply

  override val name: String = "HTTP"

  override def up(implicit system: ActorSystem, materializer: Materializer): Future[akka.http.scaladsl.Http.ServerBinding] = {
    val pubSub: PubSub = PubSub(system)
    val route =
      path("http") {
        get {
          pubSub.publish("example", "Hey apple!")
          complete(StatusCodes.Created)
        }
      }
    akka.http.scaladsl.Http().bindAndHandle(
      handler = route,
      interface = interface,
      port = port,
    )
  }
}
