package live.interfaces

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import live.Subscriber.Converter
import live.{Interface, Interfaces, PubSub, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

case class Ws(port: Int, interface: String = Interfaces.everywhere) extends Interface {

  implicit val wsConverter: Converter[Message] = TextMessage.apply

  override val name: String = "WS"

  override def up(implicit system: ActorSystem, materializer: Materializer): Future[Http.ServerBinding] = {

    implicit val ec: ExecutionContext = system.dispatcher

    val pubSub = PubSub(system)

    def greeter(topic: String): Flow[Message, Message, Any] = {
      val (queue, source) = Source.queue[Message](1000, OverflowStrategy.dropNew).preMaterialize
      //system.actorOf(Subscriber.props(queue, topic, pubSub))
      val handler = system.spawnAnonymous(initial(queue, pubSub))
      val publish: Sink[Message, Future[Done]] = Sink.foreach {
        case TextMessage.Strict(msg) => handler ! (msg, topic) //pubSub.publish(topic, msg)
        case TextMessage.Streamed(_) => ??? //TODO
      }
      Flow.fromSinkAndSource(publish, source)
    }

    val websocketRoute =
      path(Segment) { topic =>
        handleWebSocketMessages(greeter(topic))
      }

    Http().bindAndHandle(
      websocketRoute,
      interface = interface,
      port = port,
      connectionContext = HttpConnectionContext())
  }

  type Msg = (String, String)

  def initial(queue: SourceQueueWithComplete[Message], pubSub: PubSub): Behavior[Msg] = Behaviors.receiveMessagePartial {
    case ("subscribir", topic) =>
      subscribed(queue, topic, pubSub)
    case _ => Behaviors.logMessages(Behaviors.same)
  }

  def subscribed(queue: SourceQueueWithComplete[Message], topic: String, pubSub: PubSub): Behavior[Msg] = Behaviors.setup { ctx =>
    val subscriber = ctx.system.toUntyped.actorOf(Subscriber.props(queue, topic, pubSub))
    Behaviors.receiveMessagePartial {
      case (in, _) =>
        subscriber ! in
        Behaviors.same
    }
  }
}