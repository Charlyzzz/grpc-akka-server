package pubsub

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestProbe}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object DistributedPubSubSpec extends MultiNodeConfig {
  val first = role("first")
  val second = role("second")

  commonConfig(ConfigFactory.parseString(
    """
    akka.loglevel = INFO
    akka.actor.provider = "cluster"
    """))
}

class PubSubMediatorMultiJvmNode1 extends DistributedPubSubSpec

class PubSubMediatorMultiJvmNode2 extends DistributedPubSubSpec


class DistributedPubSubSpec
    extends MultiNodeSpec(DistributedPubSubSpec)
        with STMultiNodeSpec
        with ImplicitSender {
  import DistributedPubSubSpec._
  import akka.cluster.pubsub.DistributedPubSubMediator._

  override def initialParticipants = roles.size

  def join(from: RoleName, to: RoleName): Unit = {
    runOn(from) {
      Cluster(system).join(node(to).address)
      createMediator()
    }
    enterBarrier(from.name + "-joined")
  }

  def createMediator(): ActorRef = DistributedPubSub(system).mediator

  def mediator: ActorRef = DistributedPubSub(system).mediator

  def awaitCount(expected: Int): Unit = {
    awaitAssert {
      mediator ! Count
      expectMsgType[Int] should ===(expected)
    }
  }

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

    val topic = "Watches"

    "publish across nodes" in within(10.seconds) {
      val subscriber = TestProbe()
      mediator ! Subscribe(topic, subscriber.ref)
      expectMsgType[SubscribeAck]
      awaitCount(2)
      //awaitCountSubscribers(2, topic)
//
//      runOn(first) {
//        mediator ! Publish(topic, "Hola")
//      }
//      enterBarrier("a")
//      expectMsg("Hola")

    }
  }
}