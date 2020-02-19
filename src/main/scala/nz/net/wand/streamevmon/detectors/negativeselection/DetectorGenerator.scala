package nz.net.wand.streamevmon.detectors.negativeselection

import java.util.concurrent.ThreadLocalRandom

import scala.collection.mutable

case class DetectorGenerator(
  dimensions: Int,
  selfData        : Iterable[Iterable[Double]],
  nonselfData     : Iterable[Iterable[Double]],
  dimensionRanges : Iterable[(Double, Double)],
  generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
) {

  private[negativeselection] def getClosestSelf(centre: Iterable[Double]): (Iterable[Double], Double) = {
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

  private def generateNaiveCentre(): Iterable[Double] = {
    dimensionRanges
      .map { range =>
        val rangeSize = math.abs(range._2 - range._1)
        val outsideBufferSize = rangeSize * generationMethod.borderProportion
        val min = range._1 - outsideBufferSize
        val max = range._2 + outsideBufferSize
        ThreadLocalRandom.current().nextDouble(min, max)
      }
  }

  private def generateNaive(): Detector = {
    val centre = generateNaiveCentre()
    val closestSelfPoint = getClosestSelf(centre)

    Detector(
      dimensions,
      centre,
      closestSelfPoint._2,
      math.pow(math.sqrt(closestSelfPoint._2) * generationMethod.detectorRedundancyProportion, 2),
      closestSelfPoint._1
    )
  }

  private def detectorFromCentre(centre: Iterable[Double]): Detector = {
    val closestSelfPoint = getClosestSelf(centre)

    Detector(
      dimensions,
      centre,
      closestSelfPoint._2,
      math.pow(math.sqrt(closestSelfPoint._2) * generationMethod.detectorRedundancyProportion, 2),
      closestSelfPoint._1
    )
  }

  private def generateWithSpatialPreference(): Detector = ???

  private def generateWithFeaturePreference(): Detector = ???

  def generateUntilDone(): Iterable[Detector] = {
    // If we're not using redundancy, just generate a few detectors since the
    // inferior coverage termination method is unimplemented.
    val initialDetectors = if (!generationMethod.redundancy) {
      Range(0, 1).map(_ => generateNaive())
    }
    // If we are using redundancy, each new detector should not be made
    // redundant by any existing detector.
    else {
      val nonRedundantDetectors: mutable.Buffer[Detector] = mutable.Buffer(
        generateNaive()
      )

      var redundantCount = 0

      while (redundantCount.toDouble / nonRedundantDetectors.size <
        generationMethod.detectorRedundancyTerminationThreshold) {

        val newCentre = generateNaiveCentre()
        if (!nonRedundantDetectors.exists(d => d.makesRedundant(newCentre))) {
          nonRedundantDetectors.append(detectorFromCentre(newCentre))
        }
        else {
          redundantCount += 1
          println(s"Found redundant detector ($redundantCount/${nonRedundantDetectors.size})", newCentre)
        }
      }

      nonRedundantDetectors
    }

    initialDetectors

    // Next, we generate some detectors with spatial preference if enabled.

    // Finally, we generate detectors with feature preference if enabled.
  }
}
