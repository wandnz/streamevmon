package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import java.io._

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

    "be convertable to InetAddress" ignore {

    }
  }
}
