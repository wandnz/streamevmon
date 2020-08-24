package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.runners.tuner.jobs.JobResult

case class NabJobResult(
  job: NabJob
) extends JobResult {
}
