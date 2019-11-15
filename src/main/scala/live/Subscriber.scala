package live

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.stream.scaladsl.SourceQueueWithComplete

case class Subscriber(queue: SourceQueueWithComplete[Event], topic: String, pubSub: PubSub2) extends Actor with ActorLogging {

  import context._

  queue.watchCompletion().onComplete(_ => stop(self))
  pubSub.subscribe(topic, self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe(topic, _, _)) =>
      log.info(s"Subscribed to $topic")
      become(listeningTopic)
    case _ => log.warning("Received msg but was waiting for ACK")
  }

  def listeningTopic: Receive = {
    case msg: String =>
      log.info(msg)
      queue.offer(Event(msg))
  }
}

object Subscriber {
  def props(queue: SourceQueueWithComplete[Event], topic: String, pubSub: PubSub2): Props = Props(new Subscriber(queue, topic, pubSub))
}
