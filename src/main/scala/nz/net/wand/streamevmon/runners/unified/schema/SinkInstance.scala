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

package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.streaming.api.functions.sink.SinkFunction

/** Represents a configuration for a sink. It just has a type and optionally
  * some override parameters.
  *
  * @param sinkType The type of sink, such as Influx or Print.
  * @param config   Any configuration overrides that should be passed to the sink.
  */
case class SinkInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SinkTypeReference])
  sinkType: SinkType.ValueBuilder,
  config  : Map[String, String] = Map()
) {
  /** Builds the appropriate sink with configuration overrides. */
  def build: SinkFunction[Event] with HasFlinkConfig = {
    val configPrefix = s"sink.$sinkType"

    sinkType
      .build
      .overrideConfig(config, configPrefix)
  }
}
