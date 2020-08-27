package nz.net.wand.streamevmon.runners.tuner.jobs

case class FailedJob(
  job: Job,
  exception: Exception
) extends JobResult
