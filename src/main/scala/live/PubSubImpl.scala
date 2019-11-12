package live

import akka.NotUsed
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler, typed}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}


class PubSubImpl(implicit mat: Materializer, actorSystem: ActorSystem) extends PubSub {
  val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val context: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val timeout: Timeout = 5.second
  implicit val scheduler: Scheduler = typedSystem.scheduler

  val mediator = DistributedPubSub(actorSystem).mediator


  override def subscribe(in: SubRequest): Source[Event, NotUsed] = {
    val (queue, source) = Source.queue[Event](1000, OverflowStrategy.dropNew).preMaterialize
    actorSystem.actorOf(Subscriber.props(queue, in.name))
    source
  }

  override def emitEvent(in: EventRequest): Future[EmitAck] = {

    mediator ! DistributedPubSubMediator.Publish(in.topic, in.event)
    val key = Topic.key(in.topic)
    val subscribers: Future[Receptionist.Listing] = typedSystem.receptionist
        .ask[Receptionist.Listing](ref => {
          Receptionist.Find(key, ref)
        })

    subscribers
        .map(_.serviceInstances(key).foreach(a => a ! in.event))
        .map(_ => EmitAck())
  }
}