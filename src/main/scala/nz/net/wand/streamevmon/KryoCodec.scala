package nz.net.wand.streamevmon

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.Kryo
import scalacache.serialization.{Codec, FailedToDecode}
import scalacache.serialization.gzip.GZippingBinaryCodec
import scalacache.serialization.Codec.DecodingResult

import scala.util.{Failure, Success, Try}

class KryoCodec extends Codec[Any] {
  @transient private val kryo = new Kryo()

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

class GZippingKryoCodec extends KryoCodec with GZippingBinaryCodec[Any]
