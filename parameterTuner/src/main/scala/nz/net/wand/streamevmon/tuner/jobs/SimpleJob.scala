package nz.net.wand.streamevmon.tuner.jobs

/** Just a simple proof of concept job that prints a message.
  */
case class SimpleJob(uid: String) extends Job(uid) {
  override def run(): JobResult = {
    logger.info(s"Hello world! uid: $uid")

    new JobResult {
      override val job: Job = SimpleJob.this
    }
  }
}
