package nz.net.wand.streamevmon.runners.tuner.nab

import nz.net.wand.streamevmon.runners.tuner.jobs.{Job, JobResult}

case class NabJob(uid: String, args: Array[String], outputDir: String) extends Job(uid) {
  override def run(): JobResult = {
    val runner = new NabAllDetectors
    runner.runOnAllNabFiles(args, outputDir)
    NabJobResult(this)
  }
}
