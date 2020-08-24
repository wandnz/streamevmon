package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.runners.tuner.jobs.SimpleJob
import nz.net.wand.streamevmon.runners.tuner.nab.NabJob

object ParameterTuner {
  def main(args: Array[String]): Unit = {
    ConfiguredPipelineRunner.addJobResultHook {
      jr => println(s"Got job result! $jr")
    }

    ConfiguredPipelineRunner.submit(SimpleJob("HelloWorld"))
    ConfiguredPipelineRunner.submit(NabJob(
      "NabJob",
      Array(
        "--detector.baseline.percentile", "0.25",
        "--detector.baseline.threshold", "50.0"
      ),
      "./out/parameterTuner/base"
    ))

    //Thread.sleep(5000)

    //ConfiguredPipelineRunner.shutdownImmediately()
  }
}
