package io.darwin.afka.services.common

import akka.actor.{Actor, ActorRef}
import io.darwin.afka.NodeId
import io.darwin.afka.packets.requests.KafkaRequest

case class ChannelAddress(nodeId: NodeId, host: String, port: Int)
case class ChannelConnected(target: ActorRef)
/**
  * Created by darwin on 4/1/2017.
  */
trait KafkaServiceSinkChannel {
  this: Actor ⇒

  def sending[A <: KafkaRequest](req: A, from: ActorRef = self)
}
