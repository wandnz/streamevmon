package nz.net.wand.streamevmon.runners.tuner

object ParameterTuner {
  def main(args: Array[String]): Unit = {
    ConfiguredPipelineRunner.addJobResultHook {
      jr => println(s"Got job result! $jr")
    }

    ConfiguredPipelineRunner.submit(Job(Map("hello" -> "world")))

    Thread.sleep(5000)

    ConfiguredPipelineRunner.shutdownImmediately()
  }
}
