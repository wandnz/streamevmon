package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.tuner.jobs.Job
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.Await

object ConfiguredPipelineRunner {

  lazy val actorSystem: ActorSystem = ActorSystem("Top-Actor-System")
  lazy val manager: ActorRef = actorSystem.actorOf(Props[ActorManager], "Top-Actor-Manager")

  def submit(jobs: Job*): Unit = {
    jobs.foreach(j => manager ! j)
  }

  def addJobResultHook(func: JobResultHook): Unit = {
    manager ! func
  }

  def removeJobResultHook(hook: RemoveJobResultHook): Unit = {
    manager ! hook
  }

  def shutdownImmediately(): Unit = Await.result(actorSystem.terminate(), Duration(1, MINUTES))
}
