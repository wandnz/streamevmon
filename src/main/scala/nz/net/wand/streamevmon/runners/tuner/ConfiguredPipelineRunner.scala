package nz.net.wand.streamevmon.runners.tuner

import nz.net.wand.streamevmon.runners.tuner.jobs.{Job, JobResult}

import akka.actor._

object ConfiguredPipelineRunner {

  lazy val actorSystem: ActorSystem = ActorSystem("Top-Actor-System")
  lazy val manager: ActorRef = actorSystem.actorOf(Props[ActorManager], "Top-Actor-Manager")

  def submit(jobs: Job*): Unit = {
    jobs.foreach(j => manager ! j)
  }

  def addJobResultHook(func: JobResult => Unit): Unit = {
    manager ! func
  }

  def shutdownImmediately(): Unit = actorSystem.terminate()
}
