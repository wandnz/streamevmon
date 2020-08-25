package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.runners.tuner.jobs.Job
import nz.net.wand.streamevmon.runners.tuner.ActorManager.JobResultHook

import akka.actor._

object ConfiguredPipelineRunner {

  lazy val actorSystem: ActorSystem = ActorSystem("Top-Actor-System")
  lazy val manager: ActorRef = actorSystem.actorOf(Props[ActorManager], "Top-Actor-Manager")

  def submit(jobs: Job*): Unit = {
    jobs.foreach(j => manager ! j)
  }

  def addJobResultHook(func: JobResultHook): Unit = {
    manager ! func
  }

  def shutdownImmediately(): Unit = actorSystem.terminate()
}
