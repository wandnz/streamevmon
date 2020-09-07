package nz.net.wand.streamevmon.tuner.nab

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration
import ca.ubc.cs.beta.aeatk.algorithmrunresult.{AbstractAlgorithmRunResult, RunStatus}

case class NabAlgorithmRunResult(
  runConfiguration: AlgorithmRunConfiguration,
  acResult: RunStatus,
  runtime: Double,
  quality: Double,
  additionalRunData: String,
  wallClockTime    : Double
) extends AbstractAlgorithmRunResult(
  runConfiguration,
  acResult,
  runtime,
  0, // we don't care about this field
  quality,
  runConfiguration.getProblemInstanceSeedPair.getSeed, // ignored field
  "", // ignored
  false, // ignored
  additionalRunData,
  wallClockTime
) {
  override def kill(): Unit = {
    // We don't bother to implement anything for this - it's best effort, and
    // can only be called in very specific contexts.
  }
}
