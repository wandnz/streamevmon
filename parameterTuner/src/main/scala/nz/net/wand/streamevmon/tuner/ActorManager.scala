package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.tuner.jobs.{Job, JobActor, JobResult}
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{Actor, Props}

import scala.collection.mutable

/** This is the top-level Actor of our ActorSystem. It controls dispatch
  * and results for all jobs in the parameterTuner module. You should not use
  * this class directly - [[ConfiguredPipelineRunner]] has methods for that.
  *
  * Code which would allow us to shutdown only after all jobs are finished can
  * be found at https://stackoverflow.com/a/48766551, but we don't currently
  * support that.
  */
class ActorManager extends Actor with Logging {

  /** All hooks are executed whenever a job result is received.
    * As such, they should probably have some sort of filtering or matching
    * involved.
    */
  val jobResultHooks: mutable.Buffer[JobResultHook] = mutable.Buffer()

  /** Called when any message is received. Depending on the item, different
    * behaviour is triggered.
    *
    * - A [[nz.net.wand.streamevmon.tuner.jobs.Job Job]] will be executed.
    * - A [[nz.net.wand.streamevmon.tuner.jobs.JobResult JobResult]] will be
    * passed to all the [[ActorManager.JobResultHook JobResultHooks]] registered.
    * - A [[ActorManager.JobResultHook JobResultHook]] will be registered to be
    * called when a JobResult is received.
    * - A [[ActorManager.RemoveJobResultHook RemoveJobResultHook]] will cause
    * the associated JobResultHook to be removed.
    */
  override def receive: Receive = {
    case j: Job =>
      // Each child goes to a new JobActor, which will execute that job and
      // then exit.
      logger.info(s"Spawning child for job $j")
      val child = context.actorOf(Props[JobActor], j.toString)
      child ! j

    case jr: JobResult =>
      // Each Job will return exactly one JobResult.
      logger.info(s"Got job result $jr")
      jobResultHooks.foreach(_ (jr))

    case func: JobResultHook =>
      // A JobResultHook hangs around until it's removed.
      logger.info(s"Adding job result hook")
      jobResultHooks.append(func)

    case remove: RemoveJobResultHook =>
      // A RemoveJobResultHook contains a reference to a JobResultHook by which
      // we can remove the hook.
      val hook = jobResultHooks.zipWithIndex.find(_._1 == remove.jrh)
      hook match {
        case Some(value) =>
          logger.info(s"Removing job result hook ${value._1.getClass.getCanonicalName}")
          jobResultHooks.remove(value._2)
        case None =>
          logger.warn(s"Failed to remove job result hook ${remove.jrh.getClass.getCanonicalName}")
      }
  }
}

object ActorManager {

  abstract class JobResultHook {
    def apply(jr: JobResult): Unit
  }

  case class RemoveJobResultHook(jrh: JobResultHook)
}
