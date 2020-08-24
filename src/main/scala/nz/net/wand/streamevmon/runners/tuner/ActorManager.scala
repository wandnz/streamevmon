package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.Logging

import akka.actor._

/** Shutdown logic at https://stackoverflow.com/a/48766551
  */
class ActorManager extends Actor with Logging {

  override def receive: Receive = {
    case j: Job =>
      logger.info(s"Spawning child for job $j")
      val child = context.actorOf(Props[JobActor], j.toString)
      child ! j

    case jr: JobResult => logger.info(s"Got job result $jr")
  }
}
