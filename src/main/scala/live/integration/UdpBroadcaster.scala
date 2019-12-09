package live.integration

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, ExtendedActorSystem, Props}
import akka.discovery.ServiceDiscovery.{Resolved, ResolvedTarget}
import akka.discovery.{Lookup, ServiceDiscovery}
import akka.io.Udp
import akka.pattern._
import akka.util.{ByteString, Timeout}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case object Scan

class UdpBroadcaster extends Actor with ActorLogging {
  implicit val system: ActorSystem = context.system
  implicit val executionContext: ExecutionContext = system.dispatcher
  val thisHostname = system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
  println(s"a -> ${thisHostname.hostPort}")
  val wellKnownPort = 6666


  Udp(context.system).manager ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", wellKnownPort), List(Udp.SO.Broadcast(true)))
  system.scheduler.schedule(0.seconds, 8.seconds, self, Scan)

  var services: Set[ResolvedTarget] = Set()

  override def receive: Receive = binding

  def binding: Receive = {
    case Udp.Bound(addr) =>
      log.info(s"bound to $addr")
      context.become(scanning(sender))
  }

  def scanning(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      log.info(s"received data from $remote")
      if (data.startsWith("AKKA-")) {
        log.info(s"$remote, ${remote.getAddress}, ${remote.getHostName}, ${remote.getAddress}")
        services = services +
          ResolvedTarget(remote.getHostName, Some(remote.getPort), InetAddress.getAllByName(remote.getHostName).headOption)
        log.info(s"$services")
      }
    case Scan =>
      log.info("Scanning")
      socket ! Udp.Send(ByteString(s"AKKA-asd"), new InetSocketAddress("255.255.255.255", wellKnownPort))
    case lookup: Lookup =>
      sender ! Resolved(lookup.serviceName, services.toList)
    case otherwise => log.warning(s"$otherwise")
  }
}

class UdpBroadcastServiceDiscovery(system: ActorSystem) extends ServiceDiscovery {

  val handler = system.actorOf(Props(new UdpBroadcaster), "udp-discovery-handler")

  override def lookup(lookup: Lookup, resolveTimeout: FiniteDuration): Future[ServiceDiscovery.Resolved] = {
    implicit val timeout = Timeout(resolveTimeout + 100.millis)
    handler.ask(lookup).mapTo[Resolved]
  }
}

object A extends App {
  val system = ActorSystem()
  val broadcaster = system.actorOf(Props(new UdpBroadcaster))
}

//Craft al revés
//Proyecto. Descripción de tecnologías con loguito
//Clientes tamaño. Priorización?
//Texto de los clientes. Detalles verdes muy fuertes
//Detalles de los pinos. Orden?
