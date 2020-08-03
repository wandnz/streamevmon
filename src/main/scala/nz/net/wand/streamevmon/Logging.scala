package nz.net.wand.streamevmon

import org.slf4j.{Logger, LoggerFactory}

/** Allows globally configurable logging.
  *
  * @example
  * {{{
  *   logger.info("An info message.")
  *   logger.error("An error message!")
  * }}}
  * @see `simplelogger.properties` file for the current configuration.
  * @see [[https://www.slf4j.org/api/org/slf4j/Logger.html]] for details on
  *      usage and output configuration.
  */
trait Logging {
  @transient protected lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
