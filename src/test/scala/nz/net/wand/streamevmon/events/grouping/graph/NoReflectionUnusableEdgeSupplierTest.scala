package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}

class NoReflectionUnusableEdgeSupplierTest extends TestBase {
  "EdgeWithLastSeenSupplier" should {
    "fail if get() is called" in {
      an[IllegalCallerException] shouldBe thrownBy(new NoReflectionUnusableEdgeSupplier().get())
    }

    "serialize via Kryo" in {
      val original = new NoReflectionUnusableEdgeSupplier[Any]()
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      k.readClassAndObject(input) shouldBe a[NoReflectionUnusableEdgeSupplier[Any]]
    }
  }
}
