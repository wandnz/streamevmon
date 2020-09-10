package nz.net.wand.streamevmon.parameters

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

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
}