package io.darwin.afka.packets.requests

import akka.util.ByteString
import io.darwin.kafka.macros.{KafkaRequestElement, KafkaRequestPacket}

/**
  * Created by darwin on 24/12/2016.
  */
@KafkaRequestElement
case class GroupProtocol
  ( name: String,     // "range"/"roundrobin"
    meta: ByteString )


@KafkaRequestPacket(apiKey = 11, version = 1)
case class JoinGroupRequest
  ( groupId:          String,
    sessionTimeout:   Int = 60000,
    rebalanceTimeout: Int = 30000,
    memberId:         String = "",
    protocolType:     String = "consumer",
    protocols:        Array[GroupProtocol])


