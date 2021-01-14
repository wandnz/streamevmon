package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import java.time.Instant

import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import com.esotericsoftware.kryo.Kryo

class EdgeWithLastSeenTest extends TestBase {
  "EdgeWithLastSeen" should {
    "serialize via Kryo" in {
      val time = Instant.now()
      val original = new EdgeWithLastSeen(time)
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      val output = k.readClassAndObject(input)
      output shouldBe an[EdgeWithLastSeen]
      output.asInstanceOf[EdgeWithLastSeen].lastSeen shouldBe time
    }
  }

  "EdgeWithLastSeenSupplier" should {
    "fail if get() is called" in {
      an[IllegalCallerException] shouldBe thrownBy(new EdgeWithLastSeenSupplier().get())
    }

    "serialize via Kryo" in {
      val original = new EdgeWithLastSeenSupplier()
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      k.readClassAndObject(input) shouldBe an[EdgeWithLastSeenSupplier]
    }
  }
}
