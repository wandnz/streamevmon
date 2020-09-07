package nz.net.wand.streamevmon.tuner.jobs

trait JobResult {
  val job: Job

  override def toString: String = s"$job:Result"
}
