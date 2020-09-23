package nz.net.wand.streamevmon.tuner.jobs

/** A simple implementation of a failed job result. It's basically an exception
  * in non-scary form.
  */
case class FailedJob(
  job: Job,
  exception: Exception
) extends JobResult
