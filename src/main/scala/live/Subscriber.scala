package live

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContextExecutor

case class Subscriber(queue: SourceQueueWithComplete[Event]) extends Actor with ActorLogging {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  queue.watchCompletion().onComplete(_ => context.stop(self))

  override def receive: Receive = {
    case msg: String =>
      log.info(msg)
      queue.offer(Event(msg))
  }
}

object Subscriber {
  def props(queue: SourceQueueWithComplete[Event]): Props = Props(new Subscriber(queue))
}
