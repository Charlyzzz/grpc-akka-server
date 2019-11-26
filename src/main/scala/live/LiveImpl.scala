package live

import akka.NotUsed
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler, typed}
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import live.Subscriber._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class LiveImpl(implicit mat: Materializer, actorSystem: ActorSystem, converter: Converter[Event]) extends Live {

  val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val timeout: Timeout = 5.second
  implicit val scheduler: Scheduler = typedSystem.scheduler

  val pubSub: PubSub = PubSub(actorSystem)

  override def subscribe(in: SubRequest): Source[Event, NotUsed] = {
    val (queue, source) = Source.queue[Event](1000, OverflowStrategy.dropNew).preMaterialize
    actorSystem.actorOf(Subscriber.props(queue, in.name, pubSub))
    source
  }

  override def emitEvent(in: EventRequest): Future[EmitAck] = {
    if (in.event.nonEmpty)
      pubSub.publish(in.topic, in.event)
    Future(EmitAck())
  }
}

