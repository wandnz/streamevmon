package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.Logging

import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Default supertype for all SourceFunctions in this project. Wraps SourceFunction
  * with a [[shutdownAll]] function, signalling all running subclasses that they
  * should gracefully exit.
  *
  * Implementing types should ensure that they check [[shouldShutdown]] frequently
  * in order to obey calls to shutdownAll.
  *
  * @tparam T The type of data emitted.
  */
abstract class StoppableSourceFunction[T] extends SourceFunction[T] with Logging {

  def shouldShutdown: Boolean = StoppableSourceFunctionCompanion.shouldShutdown

  /** Signals all running StoppableSourceFunctions to stop. Use this when you
    * want the pipeline to exit, since there doesn't appear to be a good way of
    * stopping streaming sources.
    */
  def shutdownAll(): Unit = {
    logger.info(s"Shutting down all SourceFunctions")
    StoppableSourceFunctionCompanion.shouldShutdown = true
  }
}

/** Companion object for StoppableSourceFunction.
  *
  * This can't be named StoppableSourceFunction since that breaks references to
  * SourceFunction.SourceContext.
  */
private[this] object StoppableSourceFunctionCompanion {
  // Would be great to have this at file-scope, but that's not possible.
  private[flink] var shouldShutdown = false
}
