package nz.net.wand.streamevmon.detectors.negativeselection

/** Configures detector generation. Used by [[DetectorGenerator]].
  *
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
  redundancy                            : Boolean = true,
  spatialPreference                     : Boolean = true,
  featurePreference                     : Boolean = true,
  borderProportion                      : Double = 0.1,
  detectorRedundancyProportion          : Double = 0.1,
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

    if (detectorRedundancyTerminationThreshold > 1 || detectorRedundancyTerminationThreshold < 0) {
      throw new IllegalArgumentException(
        "Detector redundancy termination threshold must be between 0 and 1."
      )
    }
  }
}
