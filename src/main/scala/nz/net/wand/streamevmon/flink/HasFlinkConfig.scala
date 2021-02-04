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

package nz.net.wand.streamevmon.flink

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

/** Inherited by items which are intended for use in a Flink pipeline.
  *
  * Allows the inheritor to set a custom Flink operator name and UID, as well
  * as providing support for overriding the global configuration.
  */
trait HasFlinkConfig {
  val flinkName: String
  val flinkUid: String
  val configKeyGroup: String

  protected var overrideParams: Option[ParameterTool] = None

  def getOverrideParams: Option[ParameterTool] = overrideParams

  def overrideConfig(
    config   : Map[String, String],
    addPrefix: String = ""
  ): this.type = {
    if (config.nonEmpty) {
      if (addPrefix == "") {
        overrideConfig(ParameterTool.fromMap(config.asJava))
      }
      else {
        overrideConfig(ParameterTool.fromMap(config.map {
          case (k, v) => (s"$addPrefix.$k", v)
        }.asJava))
      }
    }
    this
  }

  def overrideConfig(config: ParameterTool): this.type = {
    overrideParams = Some(config)
    this
  }

  def configWithOverride(config: ParameterTool): ParameterTool = {
    overrideParams.fold {
      config
    } {
      p => config.mergeWith(p)
    }
  }

  def configWithOverride(context: RuntimeContext): ParameterTool = {
    configWithOverride(context.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool])
  }
}
