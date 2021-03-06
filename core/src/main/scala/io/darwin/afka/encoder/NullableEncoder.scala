package io.darwin.afka.encoder

/**
  * Created by darwin on 24/12/2016.
  */
class NullableEncoder[A]( nullWrite: (SinkChannel, Int) => Unit )
                        ( implicit en: KafkaEncoder[A] )
  extends KafkaEncoder[Option[A]] {

  override def encode(ch: SinkChannel, o: Option[A]) = {
    if(o.isEmpty) {
      nullWrite(ch, -1)
    }
    else {
      en.encode(ch, o.get)
    }
  }

}

class NullableObjectEncoder[A](implicit en: KafkaEncoder[A])
  extends NullableEncoder[A]((chan, v) ⇒ chan.putInt(v))

class NullableArrayEncoder[A](implicit en: KafkaEncoder[Array[A]] )
  extends NullableObjectEncoder[Array[A]]
