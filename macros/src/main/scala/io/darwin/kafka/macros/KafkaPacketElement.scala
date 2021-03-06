package io.darwin.kafka.macros

import scala.annotation.compileTimeOnly
import scala.meta._
/**
  * Created by darwin on 24/12/2016.
  */

@compileTimeOnly("@KafkaPacketElement packet encoder/decoder generator")
class KafkaPacketElement extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case Term.Block(Seq(cls@Defn.Class(_, name, _, ctor, _), companion: Defn.Object)) => {
        defn
      }
      case cls@Defn.Class(_, name, _, ctor, _) => {
        val v = generateCompanion(createCodecs(name, ctor.paramss), cls, name)
        //println(v.toString())
        v
      }
      case _ =>
        println(defn.structure)
        abort("@KafkaRequestElement should be defined as a case class")
    }
  }
}