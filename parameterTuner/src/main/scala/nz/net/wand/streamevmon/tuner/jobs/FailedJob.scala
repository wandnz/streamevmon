package nz.net.wand.streamevmon.tuner.jobs

case class FailedJob(
  job: Job,
  exception: Exception
) extends JobResult
