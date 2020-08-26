package nz.net.wand.streamevmon.runners.tuner.strategies

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.runners.tuner.jobs.JobResult
import nz.net.wand.streamevmon.runners.tuner.parameters.Parameters

abstract class SearchStrategy extends Logging {

  protected var initialParameters: Option[Parameters] = None

  def setInitialParameters(params: Parameters): Unit = {
    initialParameters = Some(params)
  }

  def nextParameters(lastResults: JobResult*): Parameters
}
