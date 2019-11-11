package live

import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler, typed}
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}


class PubSubImpl(implicit mat: Materializer, actorSystem: ActorSystem) extends PubSub {
  val system: typed.ActorSystem[Nothing] = actorSystem.toTyped
  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val timeout: Timeout = 5.second
  implicit val scheduler: Scheduler = system.scheduler

  override def subscribe(in: SubRequest): Source[Event, NotUsed] = {
    val (queue, source) = Source.queue[Event](1000, OverflowStrategy.dropNew).preMaterialize
    val subscriber = actorSystem.actorOf(Subscriber.props(queue))
    system.receptionist ! Receptionist.Register(Topic.key(in.name), subscriber)
    source
  }

  override def emitEvent(in: EventRequest): Future[EmitAck] = {
    val key = Topic.key(in.topic)
    val subscribers: Future[Receptionist.Listing] = system.receptionist
      .ask[Receptionist.Listing](ref => {
        Receptionist.Find(key, ref)
      })

    subscribers
      .map(_.serviceInstances(key).foreach(a => a !  in.event))
    .map(_ => EmitAck())
  }
}