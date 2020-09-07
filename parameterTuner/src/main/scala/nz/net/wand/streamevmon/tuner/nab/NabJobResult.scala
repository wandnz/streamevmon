package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.tuner.jobs.JobResult

case class NabJobResult(
  job: NabJob,
  results: Map[String, Map[String, Double]]
) extends JobResult
