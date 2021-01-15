package nz.net.wand.streamevmon.events.grouping.graph

import java.util.function.Supplier

/** Any Edge type needs a supplier to avoid issues discussed in [[DefaultWeightedEdgeSupplier]],
  * but we never want this one to be called. Instead, instances of T should be
  * constructed manually. This should be used in cases where T does not have a
  * no-arg constructor.
  */
class NoReflectionUnusableEdgeSupplier[T] extends Supplier[T] with Serializable {
  override def get(): T =
    throw new IllegalCallerException(
      "NoReflectionUnusableEdgeSupplier.get() should never be called. Construct edges manually instead."
    )
}
