package nz.net.wand.streamevmon.tuner.jobs

import nz.net.wand.streamevmon.Logging

import org.slf4j.{Logger, LoggerFactory}

/** Parent class for jobs to be executed by a [[JobActor]]. Should be submitted
  * to the [[nz.net.wand.streamevmon.tuner.ActorManager ActorManager]] via the
  * [[nz.net.wand.streamevmon.tuner.ConfiguredPipelineRunner ConfiguredPipelineRunner]].
  */
abstract class Job(uid: String) extends Logging {

  override protected lazy val logger: Logger = LoggerFactory.getLogger(s"${getClass.getName}:$uid")

  override def toString: String = s"Job-$uid"

  /** This is the entrypoint for the job, and is likely to be excuted in a
    * separate thread to the one that submitted the job.
    */
  def run(): JobResult
}
