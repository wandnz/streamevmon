package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.tuner.jobs.Job
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.Await

/** This is a helper class for interacting with an ActorSystem, which can
  * provide event-based execution and concurrency.
  */
object ConfiguredPipelineRunner {

  lazy val actorSystem: ActorSystem = ActorSystem("Top-Actor-System")
  lazy val manager: ActorRef = actorSystem.actorOf(Props[ActorManager], "Top-Actor-Manager")

  /** Submit one or more jobs. They will be executed in the background.
    */
  def submit(jobs: Job*): Unit = {
    jobs.foreach(j => manager ! j)
  }

  /** When a job returns, any hooks added by this function will have their apply
    * method called.
    */
  def addJobResultHook(func: JobResultHook): Unit = {
    manager ! func
  }

  /** To remove a JobResultHook, just wrap it in a RemoveJobResultHook object.
    */
  def removeJobResultHook(hook: RemoveJobResultHook): Unit = {
    manager ! hook
  }

  /** This should shut down the ActorSystem, stopping any running jobs and
    * letting the JVM exit.
    */
  def shutdownImmediately(): Unit = Await.result(actorSystem.terminate(), Duration(1, MINUTES))
}
