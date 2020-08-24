package nz.net.wand.streamevmon.runners.tuner.jobs

trait JobResult {
  val job: Job

  override def toString: String = s"$job:Result"
}
