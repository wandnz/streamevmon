package nz.net.wand

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  @transient protected[this] lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
