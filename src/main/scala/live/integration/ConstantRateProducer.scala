package live.integration

import akka.stream.scaladsl.Source
import live.EventRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Failure

object ConstantRateProducer extends GrpcIntegrationClient(address = "192.168.99.103", port = 30986) {

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
    .map(EventRequest(_, "example"))
    .runForeach {
      client.emitEvent(_).onComplete {
        case Failure(e) => println(e)
        case _ =>
      }
    }
}

