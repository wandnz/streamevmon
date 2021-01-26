package nz.net.wand.streamevmon

import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import scalacache.serialization.{Codec, FailedToDecode}
import scalacache.serialization.gzip.GZippingBinaryCodec
import scalacache.serialization.Codec.DecodingResult

import scala.util.{Failure, Success, Try}

/** A serialization codec to use with Scalacache that uses Kryo as a backend,
  * since it's what Flink uses and it's better than Java serialization.
  */
class KryoCodec extends Codec[Any] {
  @transient private val kryo = new ScalaKryoInstantiator().newKryo()

  override def encode(value: Any): Array[Byte] = {
    // We start with a 4K buffer, but let it resize infinitely.
    val output = new Output(4096, -1)
    kryo.writeClassAndObject(output, value)
    output.getBuffer
  }

  override def decode(data: Array[Byte]): DecodingResult[Any] = {
    val input = new Input(data)
    Try(kryo.readClassAndObject(input)) match {
      case Failure(exception) => Left(FailedToDecode(exception))
      case Success(value) => if (value == null) {
        Left(FailedToDecode(new IllegalArgumentException("Result of Kryo decoding was null!")))
      }
      else {
        Right(value)
      }
    }
  }
}

/** Same as [[KryoCodec]], but applies GZip for items larger than a certain
  * threshold.
  */
class GZippingKryoCodec extends KryoCodec with GZippingBinaryCodec[Any]
