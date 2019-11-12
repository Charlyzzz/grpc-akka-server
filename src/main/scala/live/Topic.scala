package live

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

sealed trait TopicAction

case class AddSubscriber(queue: ActorRef[String]) extends TopicAction

case class Broadcast(msg: String) extends TopicAction

case class AvailableSubscribers(listing: Receptionist.Listing, msg: String) extends TopicAction

object Topic {

  def apply(topicName: String): Behavior[TopicAction] = Behaviors.setup[TopicAction] { context =>
    Behaviors.receiveMessage {
      case AddSubscriber(subscriber) =>
        context.system.receptionist ! Receptionist.Register(key(topicName), subscriber)
        Behaviors.same
      case Broadcast(msg) =>
        val availableSubscribersAdapter = context.messageAdapter[Receptionist.Listing](subscribers => AvailableSubscribers(subscribers, msg))
        context.system.receptionist ! Receptionist.Find(key(topicName), availableSubscribersAdapter)
        Behaviors.same
      case AvailableSubscribers(listing, msg) =>
        listing.serviceInstances(key(topicName)).foreach(_ ! msg)
        Behaviors.same
    }
  }

  def key(topicName: String): ServiceKey[String] = ServiceKey[String](topicName)


}
