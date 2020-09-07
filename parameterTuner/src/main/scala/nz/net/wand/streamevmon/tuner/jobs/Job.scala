package nz.net.wand.streamevmon.tuner.jobs

import nz.net.wand.streamevmon.Logging

import org.slf4j.{Logger, LoggerFactory}

abstract class Job(uid: String) extends Logging {

  override protected lazy val logger: Logger = LoggerFactory.getLogger(s"${getClass.getName}:$uid")

  override def toString: String = s"Job-$uid"

  def run(): JobResult
}
