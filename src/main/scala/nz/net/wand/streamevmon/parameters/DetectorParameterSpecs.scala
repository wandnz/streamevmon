package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.parameters.constraints.ParameterConstraint.ComparableConstraint
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

/** A collection of helper methods to extract the parameters from all supported
  * detectors.
  *
  * Does not currently support the Loss detector.
  */
object DetectorParameterSpecs {
  val getAllDetectorParameters: Seq[ParameterSpec[Any]] = Seq(
    BaselineDetector.parameterSpecs,
    ChangepointDetector.parameterSpecs.asInstanceOf[Seq[ParameterSpec[Any]]],
    DistDiffDetector.parameterSpecs,
    ModeDetector.parameterSpecs,
    SpikeDetector.parameterSpecs
  ).flatten

  val fixedParameters: Map[String, Any] = Map(
    "detector.baseline.inactivityPurgeTime" -> Int.MaxValue,
    "detector.changepoint.inactivityPurgeTime" -> Int.MaxValue,
    "detector.distdiff.inactivityPurgeTime" -> Int.MaxValue,
    "detector.mode.inactivityPurgeTime" -> Int.MaxValue,
    "detector.spike.inactivityPurgeTime" -> Int.MaxValue,
  )

  /** Checks if all passed parameters are valid according to the individual
    * detectors' opinions of validity.
    *
    * TODO: Make detectors use their constraints for validity checking.
    */
  def parametersAreValid(params: Seq[ParameterInstance[Any]]): Boolean = {
    ChangepointDetector.parametersAreValid(params) &&
      DistDiffDetector.parametersAreValid(params) &&
      ModeDetector.parametersAreValid(params)
  }

  def parametersFromDetectorType(t: DetectorType.ValueBuilder): Seq[ParameterSpec[Any]] = {
    t match {
      case DetectorType.Baseline => BaselineDetector.parameterSpecs
      case DetectorType.Changepoint => ChangepointDetector.parameterSpecs.asInstanceOf[Seq[ParameterSpec[Any]]]
      case DetectorType.DistDiff => DistDiffDetector.parameterSpecs
      case DetectorType.Mode => ModeDetector.parameterSpecs
      case DetectorType.Spike => SpikeDetector.parameterSpecs
      case _ => throw new UnsupportedOperationException(s"Can't get parameters for $t")
    }
  }

  // We can specify Int as the return type here, but if we need any more generic
  // restrictions (like if we add a Double-typed restriction), the type annotation
  // gets ugly quickly. We would need to find a way to return the implicit
  // evidence of Ordering.
  def parameterRestrictionsFromDetectorType(t: DetectorType.ValueBuilder): Seq[ComparableConstraint[Any]] = {
    val result = t match {
      case DetectorType.Baseline => Seq()
      case DetectorType.Changepoint => ChangepointDetector.parameterRestrictions
      case DetectorType.DistDiff => DistDiffDetector.parameterRestrictions
      case DetectorType.Mode => ModeDetector.parameterRestrictions
      case DetectorType.Spike => Seq()
      case _ => throw new UnsupportedOperationException(s"Can't get parameter constraints for $t")
    }
    result.asInstanceOf[Seq[ComparableConstraint[Any]]]
  }
}
