package io.darwin.afka.services.pool

import java.net.InetSocketAddress

import akka.actor.{ActorRef, FSM, Props, Terminated}
import akka.io.Tcp.ErrorClosed
import io.darwin.afka.packets.requests.KafkaRequest
import io.darwin.afka.services.common._

import scala.concurrent.duration._

case object Echo

/**
  * Created by darwin on 2/1/2017.
  */
object BrokerConnection {

  def props( remote     : InetSocketAddress,
             clientId   : String,
             listener   : ActorRef ) = {
    Props(classOf[BrokerConnection], remote, clientId, listener)
  }

  sealed trait State
  case object DISCONNECT   extends State
  case object CONNECTING   extends State
  case object CONNECTED    extends State

  sealed trait Data
  case object Dummy extends Data

  trait Actor extends FSM[State, Data] {
    this: Actor with KafkaService {
      val listener: ActorRef
    } ⇒

    startWith(CONNECTING, Dummy)

    when(DISCONNECT, stateTimeout = 60 second) {
      case Event(StateTimeout, _) ⇒ {
        log.info("reconnect")
        reconnect
        goto(CONNECTING)
      }
    }

    when(CONNECTING, stateTimeout = 5 second) {
      case Event(ChannelConnected(_), _) ⇒ {
        listener ! WorkerOnline
        goto(CONNECTED)
      }
    }

    when(CONNECTED) {
      case Event(e:KafkaRequest, _) ⇒ {
        sending(e, sender)
        stay
      }
      case Event(ErrorClosed(cause), _) ⇒ {
        onDisconnected(cause)
      }
    }

    def onDisconnected(cause: String) = {
      listener ! WorkerOffline(cause)
      closeConnection
      goto(DISCONNECT)
    }

    whenUnhandled {
      case Event(Terminated(who), _) ⇒ {
        onDisconnected(s"${who} terminated")
      }
      case Event(Echo, _) ⇒
        sender ! Echo
        stay
    }

    override def postStop = {
      log.info(s"${self} is shutting down!")
      super.postStop
    }
  }
}

class BrokerConnection
  ( val remote   : InetSocketAddress,
    val clientId : String,
    val listener : ActorRef )
  extends BrokerConnection.Actor with KafkaService


