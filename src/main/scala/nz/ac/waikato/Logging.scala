package nz.ac.waikato

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  @transient lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
