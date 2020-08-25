package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.runners.tuner.jobs.{Job, JobActor, JobResult}
import nz.net.wand.streamevmon.runners.tuner.ActorManager.JobResultHook

import akka.actor._

import scala.collection.mutable

/** Shutdown logic at https://stackoverflow.com/a/48766551
  */
class ActorManager extends Actor with Logging {

  val jobResultHooks: mutable.Buffer[JobResultHook] = mutable.Buffer()

  override def receive: Receive = {
    case j: Job =>
      logger.info(s"Spawning child for job $j")
      val child = context.actorOf(Props[JobActor], j.toString)
      child ! j

    case jr: JobResult =>
      logger.info(s"Got job result $jr")
      jobResultHooks.foreach(_ (jr))

    case func: JobResultHook =>
      logger.info(s"Adding job result hook")
      jobResultHooks.append(func)
  }
}

object ActorManager {

  abstract class JobResultHook {
    def apply(jr: JobResult): Unit
  }

}
