package nz.net.wand.streamevmon.tuner.jobs

/** A simple parent class to be extended by various types of results from jobs.
  * Should have additional useful fields added to it.
  */
trait JobResult {
  val job: Job

  override def toString: String = s"$job:Result"
}
