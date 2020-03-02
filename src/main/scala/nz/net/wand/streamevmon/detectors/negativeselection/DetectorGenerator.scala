package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod._
import nz.net.wand.streamevmon.Logging

import java.util.concurrent.ThreadLocalRandom

import scala.collection.mutable

case class DetectorGenerator(
  dimensions: Int,
  selfData: Iterable[Iterable[Double]],
  nonselfData: Iterable[Iterable[Double]],
  dimensionRanges: Iterable[(Double, Double)],
  generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
) extends Logging {

  private[negativeselection] def getClosestSelf(centre: Iterable[Double]): (Iterable[Double], Double) = {
    if (selfData.isEmpty) {
      throw new IllegalArgumentException("No self data provided!")
    }

    // Take the item with the minimum distance, calculated by a sum of squares
    // of the distance per dimension.
    val result = selfData.map { point =>
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

    // Sqrt the radius to make it consistent later on
    (result._1, math.sqrt(result._2))
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

  private def detectorFromCentre(centre: Iterable[Double]): Option[Detector] = {
    val closestSelfPoint = getClosestSelf(centre)

    val radius = generationMethod.detectorRadiusMethod match {
      case FixedRadius(r) => r
      case NearestSelfSampleRadius(mult) => closestSelfPoint._2 * mult
    }

    val newDetector = Detector(
      dimensions,
      centre,
      math.pow(radius, 2),
      math.pow(radius * generationMethod.detectorRedundancyProportion, 2),
      closestSelfPoint._1
    )

    if (newDetector.contains(closestSelfPoint._1)) {
      None
    }
    else {
      Some(newDetector)
    }
  }

  private def generateNaiveDetector(): Detector = {
    // Depending on the radius generation method, we might make a detector which
    // contains a self-sample. This is considered invalid, so we'll try a number
    // of times until we get one where that's not the case.
    var attempts = 0
    while (attempts < generationMethod.generationAttempts) {
      val newDetector = detectorFromCentre(generateNaiveCentre())
      if (newDetector.isEmpty) {
        attempts += 1
      }
      else {
        return newDetector.get
      }
    }
    // ... or we never get one, so we just bail out.
    throw new RuntimeException(
      "Too many failed attempts to generate a detector! " +
        "Is your fixed radius too large?"
    )
  }

  private def generateNonRedundantDetectors(): Iterable[Detector] = {
    // Let's keep track of detectors which aren't redundant, since they
    // become mature once this algorithm completes.
    // We'll start off with one at random.
    var nonRedundantDetectors: mutable.Buffer[Detector] = mutable.Buffer(
      generateNaiveDetector()
    )

    // We will keep going until we hit our termination threshold - too many
    // redundant detectors, more than a certain proportion of mature detectors.
    var redundantCount = 0
    var backfilteredCount = 0
    while (redundantCount.toDouble / nonRedundantDetectors.size < generationMethod.detectorRedundancyTerminationThreshold &&
      nonRedundantDetectors.size <= 10000) {

      // Pick a new centre point at random, and see if it's redundant or not.
      val newCentre = generateNaiveCentre()

      if (nonRedundantDetectors.exists(d => d.makesRedundant(newCentre))) {
        // If it was deemed redundant, complain and get closer to our
        // termination condition.
        redundantCount += 1
        logger.trace(s"Found redundant detector ($redundantCount/${nonRedundantDetectors.size}): $newCentre")
      }
      else {
        // If it isn't redundant, great! But depending on the radius generation
        // method, it might overlap with a self-sample, which is bad. If that's
        // the case, we'll complain and just continue.
        val newDetector = detectorFromCentre(newCentre)
        if (newDetector.isEmpty) {
          logger.warn(
            s"Created non-redundant detector too close to a self sample. " +
              s"Your fixed radius might be too large if you see this message a lot. " +
              s"$newCentre"
          )
        }
        // If it was created successfully, great! Add it to the mature list.
        else {
          // Backfiltering is the method of removing old mature detectors that
          // are made redundant by a new detector. It's like applying redundancy
          // in reverse.
          if (generationMethod.backfiltering) {
            val detectorCountBeforeBackfiltering = nonRedundantDetectors.size

            nonRedundantDetectors = nonRedundantDetectors.filterNot {
              d => newDetector.get.makesRedundant(d.centre)
            }

            val backfilteredCountThisRound = detectorCountBeforeBackfiltering - nonRedundantDetectors.size
            backfilteredCount += backfilteredCountThisRound
            logger.trace(s"Backfiltering removed $backfilteredCountThisRound detectors!")
          }

          nonRedundantDetectors.append(newDetector.get)
          logger.trace(s"Non-redundant detector ${nonRedundantDetectors.size} (${redundantCount.toDouble / nonRedundantDetectors.size})")
        }
      }
    }

    if (generationMethod.backfiltering) {
      logger.info(s"$backfilteredCount detectors were backfiltered in total")
    }
    // Finally, return the mature detectors.
    nonRedundantDetectors
  }

  private def generateWithSpatialPreference(): Detector = ???

  private def generateWithFeaturePreference(): Detector = ???

  def generateUntilDone(): Iterable[Detector] = {
    // If we're not using redundancy, just generate a few detectors since the
    // inferior coverage termination method is unimplemented.
    val initialDetectors = if (!generationMethod.redundancy) {
      Range(0, 10).map(_ => generateNaiveDetector())
    }
    // If we are using redundancy, each new detector should not be made
    // redundant by any existing detector.
    else {
      generateNonRedundantDetectors()
    }

    initialDetectors

    // Next, we generate some detectors with spatial preference if enabled.

    // Finally, we generate detectors with feature preference if enabled.
  }
}
