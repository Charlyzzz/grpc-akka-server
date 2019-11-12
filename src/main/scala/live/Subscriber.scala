package live

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContextExecutor

case class Subscriber(queue: SourceQueueWithComplete[Event], topic: String) extends Actor with ActorLogging {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  val mediator = DistributedPubSub(context.system).mediator

  mediator ! DistributedPubSubMediator.Subscribe(topic, self)

  queue.watchCompletion().onComplete(_ => context.stop(self))

  override def receive: Receive = {
    case msg: String =>
      log.info(msg)
      queue.offer(Event(msg))
    case x => log.warning(s"received $x")
  }
}

object Subscriber {

  def props(queue: SourceQueueWithComplete[Event], topic: String): Props = Props(new Subscriber(queue, topic))
}
