package nz.net.wand.streamevmon.events.grouping.graph

import java.time.Instant

/** A simple edge type that holds an Instant, used to keep track of when the
  * edge was last seen to allow edge expiry.
  */
class EdgeWithLastSeen(val lastSeen: Instant)


