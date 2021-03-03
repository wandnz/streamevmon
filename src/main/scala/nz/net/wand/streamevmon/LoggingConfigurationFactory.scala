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
import java.net.URI
import java.nio.file.{Files, Path}

import org.apache.logging.log4j.core.config.{ConfigurationFactory, ConfigurationSource, Order, Configuration => Log4JConfig}
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory

import scala.collection.JavaConverters._

/** Forcibly overrides Log4j2 config file lookup.
  *
  * By registering a Log4j2 Plugin of type ConfigurationFactory with a high
  * priority, we can override the default behaviour of searching environment
  * variables for logging configuration files.
  *
  * We do still end out using the configuration that Flink would load, but we
  * create a
  * [[org.apache.logging.log4j.core.config.composite.CompositeConfiguration CompositeConfiguration]]
  * which allows streamevmon config files to
  * override anything set by the Flink config files.
  *
  * @see [[https://logging.apache.org/log4j/2.x/manual/customconfig.html]]
  */
@Plugin(name = "StreamevmonConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
class LoggingConfigurationFactory extends ConfigurationFactory {

  /** Log4j2 treats factories that support all types specially, allowing them
    * to fully override all other factory methods.
    */
  override def getSupportedTypes: Array[String] = Array("*")

  /** When Log4j2 finds a factory that supports all types, it calls this
    * function with the configLocation it's trying to load. In our case, it's
    * null, so the method from ConfigurationFactory returns null. We don't want
    * that behaviour, so we ignore the parameter and redirect straight to the
    * other getConfiguration implementation.
    */
  override def getConfiguration(loggerContext: LoggerContext, name: String, configLocation: URI): Log4JConfig = {
    if (isActive) {
      getConfiguration(loggerContext, ConfigurationSource.NULL_SOURCE)
    }
    else {
      null
    }
  }

  /** Creates the
    * [[org.apache.logging.log4j.core.config.composite.CompositeConfiguration CompositeConfiguration]]
    * that we want Log4j2 to use, regardless of the ConfigurationSource we got
    * passed.
    */
  override def getConfiguration(loggerContext: LoggerContext, source: ConfigurationSource): CompositeConfiguration = {
    val factory = new PropertiesConfigurationFactory

    val conf = new CompositeConfiguration(Seq(
      factory.getConfiguration(loggerContext, LoggingConfigurationFactory.getFlinkLoggerConfig),
      factory.getConfiguration(loggerContext, LoggingConfigurationFactory.getLoggerConfig)
    ).asJava)

    conf.setName(s"StreamevmonLogging@${loggerContext.getName}")

    conf
  }
}

/** Config file discovery utilities.
  *
  * It would be better for these to be in [[Configuration]], but that class
  * depends on Logging, so a circular dependency would arise. This means we
  * duplicate the `baseConfigDirectory` val.
  */
object LoggingConfigurationFactory {

  lazy val baseConfigDirectory: String = {
    if (new File("conf").exists) {
      "conf"
    }
    else {
      "/etc/streamevmon"
    }
  }

  lazy val log4jConfigFile: String = "streamevmon-log4j.properties"

  lazy val flinkConfigDirectory: String = "/etc/flink"

  lazy val flinkLoggerConfigFile: String = "log4j-console.properties"
  lazy val flinkLoggerConfigFileInternalName: String = "flink-log4j-console.properties"

  /** Gets a log4j ConfigurationSource representing the system's Flink logging
    * config, or the bundled one as fallback.
    *
    * Inspects the `log4j.configurationFile` environment variable first, since
    * that's what flink-console.sh uses. Secondly, checks
    * `/usr/share/flink/conf/flink-log4j-console.properties`. Finally loads the
    * internal config.
    */
  def getFlinkLoggerConfig: ConfigurationSource = {
    if (sys.env.get("log4j.configurationFile").exists(f => Files.exists(Path.of(f)))) {
      println(s"Loading Flink log config file from ${sys.env("log4j.configurationFile")}")
      ConfigurationSource.fromUri(Path.of(sys.env("log4j.configurationFile")).toUri)
    }
    else if (Files.exists(Path.of(flinkConfigDirectory, flinkLoggerConfigFile))) {
      println(s"Loading Flink log config file from ${Path.of(flinkConfigDirectory, flinkLoggerConfigFile)}")
      ConfigurationSource.fromUri(Path.of(flinkConfigDirectory, flinkLoggerConfigFile).toUri)
    }
    else {
      println(s"env:log4j.configurationFile = ${sys.env.get("log4j.configurationFile")}")
      println(s"Could not find file ${Path.of(flinkConfigDirectory, flinkLoggerConfigFile)}")
      println(s"Loading Flink log config file from streamevmon-internal defaults")
      ConfigurationSource.fromResource(flinkLoggerConfigFileInternalName, getClass.getClassLoader)
    }
  }

  /** Gets a log4j ConfigurationSource representing the system configuration for
    * this program's logging. /etc/streamevmon is tried first, then ./conf, then
    * the bundled config as fallback.
    */
  def getLoggerConfig: ConfigurationSource = {
    val configFileLocation = Path.of(baseConfigDirectory, log4jConfigFile)
    if (Files.exists(configFileLocation)) {
      println(s"Loading log config file from $configFileLocation")
      ConfigurationSource.fromUri(configFileLocation.toUri)
    }
    else {
      println("Loading log config file from defaults")
      ConfigurationSource.fromResource(log4jConfigFile, getClass.getClassLoader)
    }
  }
}
