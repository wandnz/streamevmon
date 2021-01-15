package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import java.io._
import java.net.InetAddress

import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import com.twitter.chill.ScalaKryoInstantiator

class SerializableInetAddressTest extends TestBase {
  "SerializableInetAddress" should {
    "be serializable" when {
      "using Java serialization" in {
        val original = SerializableInetAddress(Array(15, 20, 25, 30))
        val serialized = new ByteArrayOutputStream()
        new ObjectOutputStream(serialized).writeObject(original)
        val deserialized = new ObjectInputStream(new ByteArrayInputStream(serialized.toByteArray)).readObject()

        deserialized shouldBe original
      }

      "using Kryo" in {
        val original = SerializableInetAddress(Array(15, 20, 25, 30))
        val inst = new ScalaKryoInstantiator()
        inst.setRegistrationRequired(false)
        val k = inst.newKryo()

        val holder = new ByteBufferOutput(1024)
        k.writeClassAndObject(holder, original)
        val input = new ByteBufferInput(holder.getByteBuffer)
        input.setPosition(0)
        val output = k.readClassAndObject(input)
        output shouldBe original
      }
    }

    "be interchangeable with InetAddress" when {
      "an InetAddress is provided" in {
        SerializableInetAddress.inetToSerializable(
          InetAddress.getByAddress(Array(1, 2, 3, 4))
        ) shouldBe SerializableInetAddress(Array(1, 2, 3, 4))

        SerializableInetAddress.optionInetToOptionSerializable(
          Some(InetAddress.getByAddress(Array(1, 2, 3, 4)))
        ) shouldBe Some(SerializableInetAddress(Array(1, 2, 3, 4)))

        SerializableInetAddress.optionInetToOptionSerializable(None) shouldBe None
      }

      "a SerializableInetAddress is provided" in {
        SerializableInetAddress.serializableToInet(
          SerializableInetAddress(Array(1, 2, 3, 4))
        ) shouldBe InetAddress.getByAddress(Array(1, 2, 3, 4))

        SerializableInetAddress.optionSerializableToOptionInet(
          Some(SerializableInetAddress(Array(1, 2, 3, 4)))
        ) shouldBe Some(InetAddress.getByAddress(Array(1, 2, 3, 4)))

        SerializableInetAddress.optionSerializableToOptionInet(None) shouldBe None
      }
    }
  }
}
