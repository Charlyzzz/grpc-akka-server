package pubsub

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestProbe}
import com.typesafe.config.ConfigFactory
import live.PubSub

import scala.concurrent.duration._

object DistributedPubSubSpec extends MultiNodeConfig {
  val first = role("first")
  val second = role("second")

  commonConfig(ConfigFactory.parseString(
    """
      |
      |akka.loglevel = INFO
      |akka.actor.provider = cluster
      |
      |""".stripMargin))
}

class PubSubMediatorMultiJvmNode1 extends DistributedPubSubSpec

class PubSubMediatorMultiJvmNode2 extends DistributedPubSubSpec

class DistributedPubSubSpec extends MultiNodeSpec(DistributedPubSubSpec) with STMultiNodeSpec with ImplicitSender {

  import DistributedPubSubSpec._
  import akka.cluster.pubsub.DistributedPubSubMediator._

  override def initialParticipants = roles.size

  def join(from: RoleName, to: RoleName): Unit = {
    runOn(from) {
      Cluster(system).join(node(to).address)
    }
    enterBarrier(from.name + "-joined")
  }

  def createMediator(): ActorRef = DistributedPubSub(system).mediator

  def awaitCount(expected: Int): Unit = {
    awaitAssert {
      mediator ! Count
      expectMsgType[Int] should ===(expected)
    }
  }

  def mediator: ActorRef = DistributedPubSub(system).mediator

  def awaitCountSubscribers(expected: Int, topic: String): Unit = {
    awaitAssert {
      mediator ! CountSubscribers(topic)
      expectMsgType[Int] should ===(expected)
    }
  }

  "A DistributedPubSub layer" must {
    "startup 2 node cluster" in within(15.seconds) {
      join(first, first)
      join(second, first)
      enterBarrier("after-1")
    }

    "publish across nodes" in within(10.seconds) {
      val subscriber = TestProbe()
      val topic = "Watches"
      PubSub(system).subscribe(topic, subscriber.ref)
      expectMsgType[SubscribeAck]
      awaitCount(2)

      val msg = "Seiko SRP777"
      runOn(first) {
        PubSub(system).publish(topic, msg)
      }

      subscriber.expectMsg(msg)
    }
  }
}
