package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import java.time.Instant

import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import com.twitter.chill.ScalaKryoInstantiator

class EdgeWithLastSeenTest extends TestBase {
  "EdgeWithLastSeen" should {
    "serialize via Kryo" in {
      val time = Instant.now()
      val original = new EdgeWithLastSeen(time)

      val inst = new ScalaKryoInstantiator()
      inst.setRegistrationRequired(false)
      val k = inst.newKryo()

      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      val output = k.readClassAndObject(input)
      output shouldBe an[EdgeWithLastSeen]
      output.asInstanceOf[EdgeWithLastSeen].lastSeen shouldBe time
    }
  }
}
