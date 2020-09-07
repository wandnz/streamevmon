package nz.net.wand.streamevmon.tuner.nab

class SmacNabJobResult(
  override val job: SmacNabJob,
  results: Map[String, Map[String, Double]],
  val smacResult  : NabAlgorithmRunResult
) extends NabJobResult(
  job, results
)
