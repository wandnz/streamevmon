package nz.net.wand.streamevmon.detectors.negativeselection

// This is really just a hypersphere.
// The length of `centre` must equal `dimensions`.
case class Detector(
  dimensions: Int,
  centre: Iterable[Double],
  squareRadius: Double,
  redundancySquareRadius: Double,
  nearestSelfpoint      : Iterable[Double]
) {

  /** If the distance between the point and the centre is less than the radius,
    * then this sphere contains the point. This can be a simple calculation
    * since the radius is the same in every dimension.
    */
  def contains(
    point: Iterable[Double],
    activeCentre: Iterable[Double] = centre,
    activeSquareRadius: Double = squareRadius
  ): Boolean = {
    val distance: Double = (point, activeCentre).zipped.map { (p, c) =>
      val difference = p - c
      difference * difference
    }.sum

    distance < activeSquareRadius
  }

  def makesRedundant(newCentre: Iterable[Double]): Boolean = {
    contains(newCentre, activeSquareRadius = redundancySquareRadius) &&
      contains(newCentre, activeCentre = nearestSelfpoint)
  }
}
