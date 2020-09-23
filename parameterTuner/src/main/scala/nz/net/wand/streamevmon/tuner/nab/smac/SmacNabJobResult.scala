package nz.net.wand.streamevmon.tuner.nab.smac

import nz.net.wand.streamevmon.tuner.nab.NabJobResult

/** This is just a NabJobResult that also includes SMAC's version of a result.
  */
class SmacNabJobResult(
  override val job: SmacNabJob,
  results: Map[String, Map[String, Double]],
  val smacResult  : NabAlgorithmRunResult
) extends NabJobResult(
  job, results
)
