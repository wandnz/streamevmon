package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.runners.tuner.jobs.{JobResult, SimpleJob}
import nz.net.wand.streamevmon.runners.tuner.nab.{NabJob, NabJobResult}
import nz.net.wand.streamevmon.runners.unified.schema.DetectorType

object ParameterTuner {
  def main(args: Array[String]): Unit = {
    ConfiguredPipelineRunner.addJobResultHook {
      jr: JobResult => {
        println(s"Got job result! $jr")
        jr match {
          case NabJobResult(_, results) => println(results)
          case r => println(s"Not a NabJobResult: $r")
        }
      }
    }

    ConfiguredPipelineRunner.submit(SimpleJob("HelloWorld"))
    ConfiguredPipelineRunner.submit(NabJob(
      "NabJob",
      Array(
        "--detector.baseline.percentile", "0.25",
        "--detector.baseline.threshold", "50.0"
      ),
      "./out/parameterTuner/base",
      detectors = Seq(
        DetectorType.Baseline
      ),
      skipDetectors = false,
      skipScoring = false
    ))

    //Thread.sleep(5000)

    //ConfiguredPipelineRunner.shutdownImmediately()
  }
}
