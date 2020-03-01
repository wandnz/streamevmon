package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod.{DetectorRadiusMethod, NearestSelfSampleRadius}

/** Configures detector generation. Used by [[DetectorGenerator]].
  *
  * @param detectorRadiusMethod                   How to determine the radius of a new detector.
  *                                               FixedRadius accepts an argument, while NearestSelfSampleRadius
  *                                               gives the radius just large enough to stop short of touching
  *                                               the nearest self-sample, then scales it down by the proportion
  *                                               in the argument. This argument must be between 0 and 1.
  * @param generationAttempts                     If generation detection fails because of the radius
  *                                               method, we should retry up to this number of times.
  * @param redundancy                             Whether to use redundancy testing. If false, a fixed
  *                                               number of detectors are generated since coverage-based
  *                                               termination is not implemented.
  * @param spatialPreference                      Whether to generate additional detectors according
  *                                               to spatial preference.
  * @param featurePreference                      Whether to generate additional detectors according
  *                                               to feature preference.
  * @param borderProportion                       How large an area around the known feature space to
  *                                               generate detectors in. For example, if there are
  *                                               known measurements in the range [0,10] and this
  *                                               parameter is set to 0.4, detectors can generate with
  *                                               their centres in the range [-4,14].
  * @param detectorRedundancyProportion           The size of the redundant-judgement zone.
  *                                               This corresponds with Rc in the paper,
  *                                               described in section 3.1.2. Must be
  *                                               between 0 and 1. A larger value means
  *                                               fewer detectors will be generated, since
  *                                               they will overlap less.
  * @param detectorRedundancyTerminationThreshold What proportion of generated
  *                                               detectors must be redundant
  *                                               before detector generation is
  *                                               halted. Must be between 0 and 1.
  *                                               A larger value means more
  *                                               detectors will be generated.
  */
case class DetectorGenerationMethod(
  detectorRadiusMethod: DetectorRadiusMethod = NearestSelfSampleRadius(),
  generationAttempts: Int = 100,
  redundancy: Boolean = true,
  backfiltering: Boolean = true,
  spatialPreference: Boolean = true,
  featurePreference: Boolean = true,
  borderProportion: Double = 0.1,
  detectorRedundancyProportion: Double = 0.1,
  detectorRedundancyTerminationThreshold: Double = 0.9
) {
  // We'll do some configuration sanity checking here, so that any exceptions
  // will be thrown at config construction rather than later.
  if (redundancy) {
    if (detectorRedundancyProportion > 1 || detectorRedundancyProportion < 0) {
      throw new IllegalArgumentException(
        "Detector redundancy proportion must be between 0 and 1."
      )
    }

    if (detectorRedundancyTerminationThreshold < 0) {
      throw new IllegalArgumentException(
        "Detector redundancy termination threshold must be greater than 0."
      )
    }
  }
}

object DetectorGenerationMethod {

  sealed trait DetectorRadiusMethod

  case class FixedRadius(radius: Double) extends DetectorRadiusMethod

  case class NearestSelfSampleRadius(radiusMultiplier: Double = 0.999) extends DetectorRadiusMethod {
    if (radiusMultiplier > 1 || radiusMultiplier < 0) {
      throw new IllegalArgumentException(
        "Detector radius multiplier for nearest self-sample approach must be between 0 and 1."
      )
    }
  }

}
