package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput}
import org.jgrapht.graph.DefaultWeightedEdge

class DefaultWeightedEdgeSupplierTest extends TestBase {
  // We can't run any equality tests here. Neither DefaultWeightedEdge or the
  // Supplier implement equals(). Second, the fields that we'd want to test on
  // the edge are private, and the supplier just doesn't have any fields.
  "DefaultWeightedEdgeSupplier" should {
    "create a DefaultWeightedEdge" in {
      new DefaultWeightedEdgeSupplier().get() shouldBe a[DefaultWeightedEdge]
    }

    "serialize via Kryo" in {
      val original = new DefaultWeightedEdgeSupplier()
      val k = new Kryo()
      val holder = new ByteBufferOutput(1024)
      k.writeClassAndObject(holder, original)
      val input = new ByteBufferInput(holder.getByteBuffer)
      input.setPosition(0)
      k.readClassAndObject(input) shouldBe a[DefaultWeightedEdgeSupplier]
    }
  }
}
