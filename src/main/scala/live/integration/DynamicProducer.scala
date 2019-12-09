package live.integration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.io.Udp
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, StreamConverters}
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.{NotUsed, actor}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object DynamicProducer extends App {

  def times[T](n: Int)(action: => T): Unit = 1.to(n).foreach { _ => action }

  Udp.SO.Broadcast

  val address = "aa3613ab0c22a44e2b1081b06b4667bd-1333606730.us-east-1.elb.amazonaws.com"
  val port = 80

  val producer = Behaviors.setup[NotUsed] { ctx =>
    implicit val executionContext: ExecutionContextExecutor = ctx.executionContext
    implicit val system: actor.ActorSystem = ctx.system.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    ctx.log.info("Spawned a producer!")
    //val clientSettings = GrpcClientSettings.connectToServiceAt(address, port).withTls(false)
    //val client = LiveClient(clientSettings)
    val http = Http(ctx.system.toUntyped)
    val request: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = http.cachedHostConnectionPool[Int]("aa3613ab0c22a44e2b1081b06b4667bd-1333606730.us-east-1.elb.amazonaws.com",
      80, ConnectionPoolSettings.default.withMaxConnections(32000))
    val ks = Source.repeat((HttpRequest(uri = "/http"), 1))
        .throttle(100, 1.second)
        .via(request)
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.foreach { a =>
          a._1 match {
            case Failure(e) =>
              println(e)
              ctx.stop(ctx.self)
            case _ =>
          }
        })(Keep.left)
        .run()
    Behaviors.receiveSignal {
      case (context, PostStop) =>
        context.log.info("Stopped")
        ks.shutdown()
        Behaviors.same
    }
  }

  val scalerBehavior: Behavior[Int] = Behaviors.receivePartial {
    case (ctx, desiredOps) =>
      ctx.log.info(s"Escalando a $desiredOps")
      val effectiveOps = desiredOps / 100
      val childrenBalance = ctx.children.size - effectiveOps
      ctx.log.info(s"Children: ${
        ctx.children.size
      }")
      ctx.log.info(s"Balance: $childrenBalance")

      if (childrenBalance > 0) {
        ctx.children.take(childrenBalance).foreach {
          ctx.stop
        }
      } else if (childrenBalance < 0) {
        times(childrenBalance.abs) {
          ctx.spawnAnonymous(producer)
        }
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
      .runForeach {
        scaler.tell
      }
}

