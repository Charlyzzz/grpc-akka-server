package live.integration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.{NotUsed, actor}
import live.{EventRequest, LiveClient}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object DynamicProducer extends App {

  def times[T](n: Int)(action: => T): Unit = 1.to(n).foreach { _ => action }

  val address = "a7445122ec9f5453f89b9ef8a4ee630e-658052086.us-east-1.elb.amazonaws.com"
  val port = 80

  val producer = Behaviors.setup[NotUsed] { ctx =>
    implicit val executionContext: ExecutionContextExecutor = ctx.executionContext
    implicit val system: actor.ActorSystem = ctx.system.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    ctx.log.info("Spawned a producer!")
    val clientSettings = GrpcClientSettings.connectToServiceAt(address, port).withTls(false)
    val client = LiveClient(clientSettings)

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
    Behaviors.empty
  }

  val scalerBehavior: Behavior[Int] = Behaviors.receivePartial {
    case (ctx, desiredOps) =>
      ctx.log.info(s"Escalando a $desiredOps")
      val effectiveOps = desiredOps / 100
      val childrenBalance = ctx.children.size - effectiveOps
      ctx.log.info(s"Children: ${ctx.children.size}")
      ctx.log.info(s"Balance: $childrenBalance")

      if (childrenBalance > 0) {
        ctx.children.take(childrenBalance).foreach { ctx.stop }
      } else if (childrenBalance < 0) {
        times(childrenBalance.abs) { ctx.spawnAnonymous(producer) }
      } else {
        ctx.log.info(s"Already at $effectiveOps")
      }
      Behaviors.same
  }

  val scaler = ActorSystem(scalerBehavior, "scaler")
  implicit val system: actor.ActorSystem = scaler.sys.toUntyped
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  StreamConverters.fromInputStream(() => System.in)
    .map(_.utf8String.filter(_ >= ' '))
    .map(in => Try(in.toInt))
    .collect {
      case Success(value) => value
    }
    .runForeach { scaler.tell }
}

