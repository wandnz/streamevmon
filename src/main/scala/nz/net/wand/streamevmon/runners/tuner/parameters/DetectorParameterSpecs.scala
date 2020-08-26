package nz.net.wand.streamevmon.runners.tuner.parameters

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector

object DetectorParameterSpecs {
  val getAllDetectorParameters: Seq[ParameterSpec[Any]] = Seq(
    BaselineDetector.parameterSpecs,
    ChangepointDetector.parameterSpecs.asInstanceOf[Seq[ParameterSpec[Any]]],
    DistDiffDetector.parameterSpecs,
    ModeDetector.parameterSpecs,
    SpikeDetector.parameterSpecs
  ).flatten

  val fixedParameters: Map[String, Any] = Map(
    "detectors.baseline.threshold" -> 0,
    "detectors.baseline.inactivityPurgeTime" -> Int.MaxValue,
    "detectors.changepoint.severityThreshold" -> 0,
    "detectors.changepoint.inactivityPurgeTime" -> Int.MaxValue,
    "detectors.distdiff.inactivityPurgeTime" -> Int.MaxValue,
    "detectors.mode.inactivityPurgeTime" -> Int.MaxValue,
    "detectors.spike.inactivityPurgeTime" -> Int.MaxValue,
  )
}
