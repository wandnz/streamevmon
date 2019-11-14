package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.Logging

/** Default supertype for all SourceFunctions in this project. Adds a
  * shutdownAll function, signalling all running subclasses that they should
  * gracefully exit.
  *
  * Implementing types should ensure that they check shouldShutdown frequently
  * in order to obey calls to shutdownAll.
  *
  * Since Flink doesn't ensure that a particular function runs on a particular
  * machine, and this is done via a static variable rather than a broadcasted
  * state, it's only really useful in a test scenario where we want to end the
  * execution of a long-running function early.
  */
trait GloballyStoppableFunction extends Logging {

  def shouldShutdown: Boolean = GloballyStoppableFunction.shouldShutdown

  /** Signals all running StoppableSourceFunctions to stop. Use this when you
    * want the pipeline to exit, since there doesn't appear to be a good way of
    * stopping streaming sources.
    */
  def shutdownAll(): Unit = {
    logger.info(s"Shutting down all SourceFunctions")
    GloballyStoppableFunction.shouldShutdown = true
  }
}

/** Companion object for GloballyStoppableFunction, containing the shared flag. */
private[this] object GloballyStoppableFunction {
  private[flink] var shouldShutdown = false
}
