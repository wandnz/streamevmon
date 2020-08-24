package nz.net.wand.streamevmon.runners.tuner

import akka.actor.Actor

class JobActor extends Actor {
  override def receive: Receive = {
    case j: Job => sender ! j.run(); context.stop(self)
    case m => println(s"Unknown message $m!")
  }
}
