package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.tuner.jobs.JobResult

/** This bundles the raw results from the NAB scorer: All three ScoreTargets for
  * every detector we're working on.
  */
case class NabJobResult(
  job: NabJob,
  results: Map[String, Map[String, Double]]
) extends JobResult
