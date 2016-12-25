package io.darwin.afka.packets.requests

import io.darwin.kafka.macros.KafkaRequestPacket

/**
  * Created by darwin on 24/12/2016.
  */
@KafkaRequestPacket(apiKey = 1, version = 1)
case class MetaDataRequest
( val topics: Option[Array[String]] )


