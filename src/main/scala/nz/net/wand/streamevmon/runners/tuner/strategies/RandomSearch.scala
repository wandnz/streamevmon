package nz.net.wand.streamevmon.runners.tuner.strategies

import nz.net.wand.streamevmon.runners.tuner.jobs.JobResult
import nz.net.wand.streamevmon.runners.tuner.parameters.{Parameter, ParameterInstance, Parameters}

case class RandomSearch(
  parameters: Iterable[Parameter[Any]]
) extends SearchStrategy {
  override def nextParameters(lastResults: JobResult*): Parameters = {
    val newParams: Seq[ParameterInstance[Any]] = parameters.map(_.generateRandomInRange()).toSeq
    new Parameters(newParams: _*)
  }
}
