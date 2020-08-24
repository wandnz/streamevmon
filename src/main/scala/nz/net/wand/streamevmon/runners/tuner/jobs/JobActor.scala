package nz.net.wand.streamevmon.runners.tuner.jobs

import nz.net.wand.streamevmon.Logging

import akka.actor.Actor

class JobActor extends Actor with Logging {
  override def receive: Receive = {
    case j: Job => sender ! j.run(); context.stop(self)
    case m => logger.error(s"Unknown message $m!")
  }
}
