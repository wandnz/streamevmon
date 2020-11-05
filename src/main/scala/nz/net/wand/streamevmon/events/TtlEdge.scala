package nz.net.wand.streamevmon.events

import scala.collection.mutable

class TtlEdge(initialTtls: TtlEntry*) {
  val ttls: mutable.Buffer[TtlEntry] = mutable.Buffer(initialTtls: _*)

  def getWeight: Double = ttls.map(_.ttl).sum / ttls.size

  def canEqual(other: Any): Boolean = other.isInstanceOf[TtlEdge]

  override def equals(other: Any): Boolean = other match {
    case that: TtlEdge =>
      (that canEqual this) &&
        ttls == that.ttls
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(ttls)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
