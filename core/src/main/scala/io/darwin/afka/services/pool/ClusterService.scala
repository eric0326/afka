package io.darwin.afka.services.pool

import java.net.InetSocketAddress

import akka.actor.{ActorRef, FSM, Props}
import io.darwin.afka.TopicId
import io.darwin.afka.domain.Brokers
import io.darwin.afka.packets.requests.{GroupCoordinateRequest, KafkaRequest, MetaDataRequest}
import io.darwin.afka.packets.responses.MetaDataResponse
import io.darwin.afka.services.common._

/**
  * Created by darwin on 1/1/2017.
  */
object ClusterService {

  def props( clusterId  : String,
             bootstraps : Array[InetSocketAddress],
             listener   : ActorRef) = {
    Props(classOf[ClusterService], clusterId, bootstraps, listener)
  }

  sealed trait State
  case object BOOTSTRAP extends State
  case object READY     extends State

  sealed trait Data
  case object Dummy extends Data

  case class  CreateConsumer(groupId: String, topics: Array[TopicId])
  case object ClusterReady
}

import ClusterService._

class ClusterService(val clusterId  : String,
                     val bootstraps : Array[InetSocketAddress],
                     val listener   : ActorRef)
  extends FSM[State, Data]
{
  log.info("bootstrapping...")
  var bootstrap = context.actorOf(BootStrapService.props(bootstraps, self))
  var connection: Option[ActorRef] = None

  private def doSend[A <: KafkaRequest](req: A, who: ActorRef)(sending : (ActorRef, Any) ⇒ Unit): Boolean = {
    connection.fold(false) { c ⇒
      sending(c, RequestPacket(req, who))
      true
    }
  }

  private def send[A <: KafkaRequest](req: A, who: ActorRef = sender()): Boolean = {
    doSend(req, who)((c, m) ⇒ c ! m)
  }

  private def forward[A <: KafkaRequest](req: A, who: ActorRef = sender()): Boolean = {
    doSend(req, who)((c, m) ⇒ c.forward(m))
  }

  //val consumers: Map[String, (CreateConsumer, ActorRef)] = Map.empty
  var brokers: Brokers = Brokers(Array.empty)

  startWith(BOOTSTRAP, Dummy)

  when(BOOTSTRAP) {
    case Event(meta:MetaDataResponse, Dummy) ⇒ {
      onBootstrapped(meta)
    }
  }

  when(READY) {
    case Event(WorkerOnline, Dummy) ⇒ {
      log.info(s"custer ${clusterId} is ready!")
      listener ! ClusterReady
      stay
    }
    case Event(e: MetaDataRequest, _) ⇒ {
      if(!send(e)) {
        sender ! NotReady(e)
      }
      stay
    }
    case Event(e: GroupCoordinateRequest, _) ⇒  {
      forward(e)
      stay
    }
    case Event(ResponsePacket(e: MetaDataResponse, req: RequestPacket), _) ⇒ {
      val newBrokers = Brokers(e.brokers)
      if(brokers != newBrokers) {
        log.info("new brokers are different")
        brokers = newBrokers
        connection.get ! brokers
      }

      req.who ! e

      stay
    }
  }

  def onBootstrapped(meta: MetaDataResponse) = {
    context stop bootstrap

    brokers = Brokers(meta.brokers)

    connection = Some(context.actorOf(BrokerService.props(
          brokers  = brokers,
          clientId = "push-service",
          listener = self),
      "broker-service"))

    goto(READY)
  }

}

