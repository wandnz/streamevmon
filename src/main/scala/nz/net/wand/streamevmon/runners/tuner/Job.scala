package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.Logging

case class Job(
  parameters: Map[String, String]
) extends Logging {

  override def toString: String = s"Job-${parameters.keys.head}"

  def run(): JobResult = {
    logger.info(s"Hello world! Params: $parameters")
    JobResult("Yay!")
  }
}
