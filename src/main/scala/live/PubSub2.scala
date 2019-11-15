package live

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

class PubSub2(private val mediator: ActorRef) {

  def publish(topic: String, event: String): Unit = {
    mediator ! DistributedPubSubMediator.Publish(topic, event)
  }

  def subscribe(topic: String, who: ActorRef): Unit = {
    implicit val sender = who
    mediator ! DistributedPubSubMediator.Subscribe(topic, who)
  }

}

object PubSub2 {
  def apply(actorSystem: ActorSystem): PubSub2 = new PubSub2(DistributedPubSub(actorSystem).mediator)
}