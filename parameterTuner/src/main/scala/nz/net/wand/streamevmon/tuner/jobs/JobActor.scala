package nz.net.wand.streamevmon.tuner.jobs

import nz.net.wand.streamevmon.Logging

import akka.actor.Actor

/** Executes a [[Job]], then exits. Any other message is logged and discarded. */
class JobActor extends Actor with Logging {
  override def receive: Receive = {
    case j: Job => sender ! j.run(); context.stop(self)
    case m => logger.error(s"Unknown message $m!")
  }
}
