package nz.net.wand.streamevmon.detectors.negativeselection

import java.util.concurrent.ThreadLocalRandom

case class DetectorGenerator(
  dimensions: Int,
  selfData        : Iterable[Iterable[Double]],
  nonselfData: Iterable[Iterable[Double]],
  dimensionRanges : Seq[(Double, Double)],
  generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
) {

  def getClosestSelf(centre: Seq[Double]): (Iterable[Double], Double) = {
    // Take the item with the minimum distance, calculated by a sum of squares
    // of the distance per dimension.
    selfData.map { point =>
      (point, centre).zipped.map { (pointDim, centreDim) =>
        // For each dimension, get the squared difference
        val difference = pointDim - centreDim
        (point, difference * difference)
      }
        // For each dimension, sum the squared differences
        .reduce((a, b) => (a._1, a._2 + b._2))
    }
      // Find the real distance by sqrting the sum of squared differences
      .map(d => (d._1, math.sqrt(d._2)))
      // Get the closest one from all points
      .minBy(_._2)
  }

  private def generateNaive(): Detector = {
    val centre = dimensionRanges
      .map(range => ThreadLocalRandom.current().nextDouble(range._1, range._2))
    val closestSelfPoint = getClosestSelf(centre)

    Detector(
      dimensions,
      centre,
      closestSelfPoint._2
    )
  }

  private def generateWithSpatialPreference(): Detector = ???

  private def generateWithFeaturePreference(): Detector = ???

  def generateUntilDone(): Iterable[Detector] = {
    // First, we generate some naive detectors.
    Range(0, 100).map(_ => generateNaive())

    // Next, we generate some detectors with spatial preference if enabled.

    // Finally, we generate detectors with feature preference if enabled.
  }
}
