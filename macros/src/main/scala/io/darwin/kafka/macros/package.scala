package io.darwin.kafka

import scala.collection.immutable.Seq
import scala.meta.{Defn, _}

/**
  * Created by darwin on 24/12/2016.
  */
package object macros {

  def createImports: Stat = {
    q"import io.darwin.afka.decoder.{ ArrayDecoder, KafkaDecoder, SourceChannel, NullableDecoder, decoding }"
  }

  def createEncoderImports: Stat = {
    q"import io.darwin.afka.encoder.{ ArrayEncoder, NullableArrayEncoder, NullableObjectEncoder, KafkaEncoder, SinkChannel, encoding }"
  }

  def createDecoderObject(name: Type.Name, paramss: Seq[Seq[Term.Param]]): Defn.Object = {

    val method = createDecodeMethod(name, paramss)

    val decoderName = name.toString + "Decoder"
    q"""
         implicit object ${Term.Name(decoderName)} extends KafkaDecoder[$name] {
            $method
         }
     """
  }

  def createDecodeMethod(name: Type.Name, paramss: Seq[Seq[Term.Param]]): Stat = {
    val args = paramss.map(_.map { param =>
      arg"""
            ${Term.Name(param.name.value)} = decoding[${Type.Name(param.decltpe.get.toString)}](${Term.Name("chan")})
        """
    })

    q"""
        override def decode(chan: SourceChannel): $name = {
           ${Ctor.Ref.Name(name.value)}(...$args)
        }
     """
  }

  def getPatVarTerm(name: String): Pat.Var.Term = {
    Pat.Var.Term(Term.Name(name))
  }

  def createOptionDecoder(name: Type.Name): Defn.Val = {
    q"""
       implicit val ${getPatVarTerm("DEC_OPTION_" + name.toString)} = NullableDecoder.make[$name]
     """
  }

  def createArrayDecoder(name: Type.Name): Defn.Val = {
    q"""
       implicit val ${getPatVarTerm("DEC_ARRAY_OF_" + name.toString)} = ArrayDecoder.make[$name]
     """
  }

  def createNullArrayDecoder(name: Type.Name): Defn.Val = {
    q"""
       implicit val ${getPatVarTerm("DEC_NULLARRAY_OF_" + name.toString)} = ArrayDecoder.makeNullable[$name]
     """
  }

  def getEncodingCode(paramss: Seq[Seq[Term.Param]]): Seq[Term.Apply] = {
    paramss.flatten.map{ param =>
      q"encoding(chan, ${Term.Name("o."+param.name.value)})"
    }
  }

  def createEncoderObject(name: Type.Name, paramss: Seq[Seq[Term.Param]]): Defn.Object = {
    val code = getEncodingCode(paramss)

    val encoderName = name.toString + "Encoder"
    q"""
         implicit object ${Term.Name(encoderName)} extends KafkaEncoder[$name] {
            override def encode(chan: SinkChannel, o: $name) = {
              ..$code
            }
         }
     """
  }

  def createArrayEncoder(name: Type.Name): Defn.Object = {
    q"""
       implicit object ${Term.Name("ARRAY_OF_" + name.toString)} extends ArrayEncoder[$name]
     """
  }

  def createNullArrayEncoder(name: Type.Name): Defn.Object = {
    q"""
       implicit object ${Term.Name("NULL_ARRAY_OF_" + name.toString)}  extends NullableArrayEncoder[$name]
      """
  }

  def createOptionEncoder(name: Type.Name): Defn.Object = {
    q"""
       implicit object ${Term.Name("OPTION_OF_" + name.toString)}  extends NullableObjectEncoder[$name]
      """
  }

  def createEncoders(name: Type.Name, paramss: Seq[Seq[Term.Param]]): Seq[Stat]= {
    val imports = createEncoderImports
    val encoder = createEncoderObject(name, paramss)
    val array = createArrayEncoder(name)
    val nullArray = createNullArrayEncoder(name)
    val option = createOptionEncoder(name)

    Seq(q"$imports", q"$encoder", q"$array", q"$nullArray", q"$option")
  }

  def createDecoders(name: Type.Name, paramss: Seq[Seq[Term.Param]]) : Seq[Stat]= {
    val imp = createImports
    val decoder = createDecoderObject(name, paramss)
    val arrayDecoder = createArrayDecoder(name)
    val nullArrayDecoder = createNullArrayDecoder(name)
    val optionDecoder = createOptionDecoder(name)
    Seq(imp, q"$decoder", q"$arrayDecoder", q"$nullArrayDecoder", q"$optionDecoder")
  }

  def createPacketDecoder(name: Type.Name, paramss: Seq[Seq[Term.Param]]): Seq[Stat] = {
    val imp = createImports
    val decoder = createDecoderObject(name, paramss)
    Seq(imp, q"$decoder")
  }

  def insertToObject(seqn: Seq[Stat], cls: Defn.Class, obj: Defn.Object): Term.Block = {
    val seqs: Seq[Stat] = seqn ++: obj.templ.stats.getOrElse(Nil)
    val newObj = obj.copy(
      templ = obj.templ.copy(stats = Some(seqs)))
    Term.Block(Seq(cls, newObj))
  }

  def createCodecs(name: Type.Name, paramss: Seq[Seq[Term.Param]]) : Seq[Stat] = {
    val imp = createImports
    val decoder = createDecoderObject(name, paramss)
    val arrayDecoder = createArrayDecoder(name)
    val nullArrayDecoder = createNullArrayDecoder(name)
    val optionDecoder = createOptionDecoder(name)

    val imports = createEncoderImports
    val encoder = createEncoderObject(name, paramss)
    val array = createArrayEncoder(name)
    val nullArray = createNullArrayEncoder(name)
    val option = createOptionEncoder(name)

    Seq(q"$imports", q"$encoder", q"$array", q"$nullArray", q"$option",
      q"$imp", q"$decoder", q"$arrayDecoder", q"$nullArrayDecoder", q"$optionDecoder")
  }

  def generateCompanion(stats: Seq[Stat], cls: Defn.Class, name: Type.Name): Term.Block = {
    val companion   =
      q"""
        object ${Term.Name(name.value)} {
          ..$stats
        }
      """
    Term.Block(Seq(cls, companion))
  }
}
