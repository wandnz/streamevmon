package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.tuner.jobs.{Job, JobActor, JobResult}
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{Actor, Props}

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

    case remove: RemoveJobResultHook =>
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
