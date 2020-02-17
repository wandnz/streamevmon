package nz.net.wand.streamevmon.detectors.negativeselection

import java.util.concurrent.ThreadLocalRandom

case class DetectorGenerator(
  dimensions: Int,
  selfData        : Iterable[Iterable[Double]],
  nonselfData: Iterable[Iterable[Double]],
  dimensionRanges : Seq[(Double, Double)],
  generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
) {

  private[negativeselection] def getClosestSelf(centre: Seq[Double]): (Iterable[Double], Double) = {
    // Take the item with the minimum distance, calculated by a sum of squares
    // of the distance per dimension.
    selfData.map { point =>
      (point, centre).zipped.map { (pointDim, centreDim) =>
        // For each dimension, get the squared difference
        val difference = pointDim - centreDim
        (point, difference * difference)
      }
        // For each item, sum the squared differences
        .reduce((a, b) => (a._1, a._2 + b._2))
    }
      // Get the closest one from all points
      .minBy(_._2)
  }

  private def generateNaive(): Detector = {
    val centre = dimensionRanges
      .map { range =>
        val rangeSize = math.abs(range._2 - range._1)
        val outsideBufferSize = rangeSize * generationMethod.borderProportion
        val min = range._1 - outsideBufferSize
        val max = range._2 + outsideBufferSize
        ThreadLocalRandom.current().nextDouble(min, max)
      }
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
