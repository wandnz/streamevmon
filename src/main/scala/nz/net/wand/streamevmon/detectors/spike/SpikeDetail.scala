package nz.net.wand.streamevmon.detectors.spike

/** Detailed output from the spike detector. This can be used to tune the
  * detector, perhaps along with a graphing library.
  *
  * @param inputValue The measurement passed to the detector.
  * @param mean       The smoothed mean value from before the measurement was passed.
  * @param lowerBound The lower bound of expected values for the current measurement.
  * @param upperBound The lower bound of expected values for the current measurement.
  * @param diff       The distance between `inputValue` and `mean`.
  * @param signal     The signal type that was emitted for this measurement.
  */
case class SpikeDetail(
  inputValue: Double,
  mean      : Double,
  lowerBound: Double,
  upperBound: Double,
  diff      : Double,
  signal: SignalType.Value
)
