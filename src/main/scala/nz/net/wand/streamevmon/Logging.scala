/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon

import java.io.File
import java.nio.file.{Files, Path}

import org.apache.logging.log4j.core.config.composite.CompositeConfiguration
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

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
  @transient protected lazy val logger: Logger = {
    Logging.configure()
    val l = LoggerFactory.getLogger(getClass)
    l.info(s"Configured logger for $getClass!")
    println(s"Configured logger $l for $getClass!")
    l
  }
}

/** Global configuration utilities for the logger. The configure() function
  * executes once, and loads log4j configs from all the correct places. The
  * `getXConfig` functions should probably be in [[Configuration]], but there's
  * a circular dependency there in that Configuration uses the Logging trait.
  * This also means we have to duplicate the `baseConfigDirectory` val.
  */
object Logging {
  lazy val baseConfigDirectory: String = {
    if (new File("conf").exists) {
      "conf"
    }
    else {
      "/etc/streamevmon"
    }
  }

  lazy val flinkConfigDirectory: String = "/usr/share/flink/conf"

  lazy val log4jConfigFile: String = "streamevmon-log4j.properties"

  lazy val flinkLoggerConfigFile: String = "flink-log4j-console.properties"

  var hasConfigured = false

  private def configure(): Unit = {
    val ctx = LogManager.getContext.asInstanceOf[LoggerContext]
    val factory = new PropertiesConfigurationFactory()

    val conf = new CompositeConfiguration(Seq(
      factory.getConfiguration(ctx, getFlinkLoggerConfig),
      factory.getConfiguration(ctx, getLoggerConfig)
    ).asJava)
    conf.setName("Streamevmon logger conf")

    ctx.setConfiguration(conf)
  }

  /** Gets a log4j ConfigurationSource representing the system's Flink logging
    * config, or the bundled one as fallback.
    */
  def getFlinkLoggerConfig: ConfigurationSource = {
    val configFileLocation = Path.of(flinkConfigDirectory, flinkLoggerConfigFile)
    if (Files.exists(configFileLocation)) {
      ConfigurationSource.fromUri(configFileLocation.toUri)
    }
    else {
      ConfigurationSource.fromResource(flinkLoggerConfigFile, getClass.getClassLoader)
    }
  }

  /** Gets a log4j ConfigurationSource representing the system configuration for
    * this program's logging. /etc/streamevmon is tried first, then ./conf, then
    * the bundled config as fallback.
    */
  def getLoggerConfig: ConfigurationSource = {
    val configFileLocation = Path.of(baseConfigDirectory, log4jConfigFile)
    if (Files.exists(configFileLocation)) {
      ConfigurationSource.fromUri(configFileLocation.toUri)
    }
    else {
      ConfigurationSource.fromResource(log4jConfigFile, getClass.getClassLoader)
    }
  }
}
