package live

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer

import scala.concurrent.Future

trait Interface {

  val name: String

  def up(implicit system: ActorSystem, materializer: Materializer): Future[Http.ServerBinding]
}

object Interfaces {

  val localhost: String = "127.0.0.0"

  val everywhere: String = "0.0.0.0"
}