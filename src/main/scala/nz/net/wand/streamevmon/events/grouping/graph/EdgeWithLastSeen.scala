package nz.net.wand.streamevmon.events.grouping.graph

import java.time.Instant
import java.util.function.Supplier

/** A simple edge type that holds an Instant, used to keep track of when the
  * edge was last seen to allow edge expiry.
  */
case class EdgeWithLastSeen(lastSeen: Instant)

/** Any Edge type needs a supplier to avoid issues discussed in [[DefaultWeightedEdgeSupplier]],
  * but we never want this one to be called. Instead, the EdgeWithLastSeen
  * constructor should be called in all cases.
  */
class EdgeWithLastSeenSupplier extends Supplier[EdgeWithLastSeen] with Serializable {
  override def get(): EdgeWithLastSeen = throw new IllegalCallerException("EdgeWithLastSeenSupplier.get() should never be called.")
}
