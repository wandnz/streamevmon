package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.Logging

import org.apache.flink.streaming.api.functions.source.RichSourceFunction

/** Default supertype for all SourceFunctions in this project. Wraps SourceFunction
  * with a [[shutdownAll]] function, signalling all running subclasses that they
  * should gracefully exit.
  *
  * Implementing types should ensure that they check [[shouldShutdown]] frequently
  * in order to obey calls to shutdownAll.
  *
  * @tparam T The type of data emitted.
  */
abstract class GloballyStoppableFunction[T] extends RichSourceFunction[T] with Logging {

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
