package example

import akka.actor.{Actor, Props}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import pubsub.STMultiNodeSpec

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("first")
  val node2 = role("second")

  commonConfig(ConfigFactory.parseString(
    """
      |
      |akka.loglevel = INFO
      |akka.actor.provider = cluster
      |
      |""".stripMargin))
}


class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample

class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample

object MultiNodeSample {

  class Ponger extends Actor {
    def receive: Receive = {
      case "ping" => sender() ! "pong"
    }
  }

}

class MultiNodeSample extends MultiNodeSpec(MultiNodeSampleConfig) with STMultiNodeSpec with ImplicitSender {

  import MultiNodeSample._
  import MultiNodeSampleConfig._

  import scala.concurrent.duration._


  def initialParticipants = roles.size

  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in within(10.seconds) {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in within(10.seconds) {
      runOn(node1) {
        enterBarrier("deployed")
        val ponger = system.actorSelection(node(node2) / "user" / "ponger")
        ponger ! "ping"
        expectMsg("pong")
      }

      runOn(node2) {
        system.actorOf(Props[Ponger], "ponger")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}