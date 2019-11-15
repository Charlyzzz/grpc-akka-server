package live

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

class PubSub(private val actorSystem: ActorSystem) {
  private val mediator = DistributedPubSub(actorSystem).mediator

  def publish(topic: String, event: String): Unit = {
    mediator ! DistributedPubSubMediator.Publish(topic, event)
  }

  def subscribe(topic: String, who: ActorRef)(implicit subscriptionAckReceiver: ActorRef = Actor.noSender): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(topic, who)
  }
}

object PubSub {
  def apply(actorSystem: ActorSystem): PubSub = new PubSub(actorSystem)
}