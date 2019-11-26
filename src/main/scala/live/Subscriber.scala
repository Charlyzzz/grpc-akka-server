package live

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.stream.scaladsl.SourceQueueWithComplete
import live.Subscriber.Converter

case class Subscriber[T](queue: SourceQueueWithComplete[T], topic: String, pubSub: PubSub, converter: Converter[T]) extends Actor with ActorLogging {

  import context._

  queue.watchCompletion().onComplete(_ => stop())
  pubSub.subscribe(topic, self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe(topic, _, _)) =>
      log.info(s"Subscribed to $topic")
      become(listeningTopic)
    case _ => log.warning("Received msg but was waiting for ACK")
  }

  def listeningTopic: Receive = {
    case msg: String =>
      log.debug(msg)
      queue.offer(converter(msg))
  }

  def stop(): Unit = {
    log.debug("Queue shut down")
    context.stop(self)
  }
}

object Subscriber {

  type Converter[T] = (String) => T

  def props[T](queue: SourceQueueWithComplete[T], topic: String, pubSub: PubSub)(implicit converter: Converter[T]): Props =
    Props(new Subscriber(queue, topic, pubSub, converter))
}

