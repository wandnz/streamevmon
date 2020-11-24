package nz.net.wand.streamevmon.events.grouping.graph

import java.util.function.Supplier

import org.jgrapht.graph.DefaultWeightedEdge

/** The built-in default edge supplier uses reflection to generate an instance
  * of the type parameter passed to the graph. Kryo refuses to serialise this
  * in JDK 9+ due to the new module system preventing reflective access to
  * certain Java core modules. Bypassing the reflective solution is a much
  * simpler approach than opening the module to reflective access.
  */
class DefaultWeightedEdgeSupplier extends Supplier[DefaultWeightedEdge] with Serializable {
  override def get(): DefaultWeightedEdge = new DefaultWeightedEdge()
}
