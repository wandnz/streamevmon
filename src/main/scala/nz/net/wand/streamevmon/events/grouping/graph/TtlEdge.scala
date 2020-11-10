package nz.net.wand.streamevmon.events.grouping.graph

import scala.collection.mutable

// TODO: We'll skip including RTT weights for now. The schema is a little
//  unclear (why do you have 400ms rtt to localhost? is it ns?), and
//  I would need to figure out how to munge the weight for a hop with
//  a NULL rtt into a Double. I don't particularly want to set a
//  default or a missing special value, since that could mess up many
//  pathfinding algorithms.
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
