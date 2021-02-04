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

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.Measurement

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.apache.flink.api.common.io.{FileInputFormat, FilePathFilter}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Represents a source. This will get built if it's required by any detectors.
  *
  * @param sourceType    The type of source, such as Influx or Esmond.
  * @param sourceSubtype The subtype of source where applicable, such as Amp or Bigdata.
  * @param config        Any configuration overrides that should be passed to the source.
  */
case class SourceInstance(
  @JsonProperty("type")
  @JsonScalaEnumeration(classOf[SourceTypeReference])
  sourceType: SourceType.ValueBuilder,
  @JsonProperty("subtype")
  @JsonScalaEnumeration(classOf[SourceSubtypeReference])
  sourceSubtype: Option[SourceSubtype.ValueBuilder],
  config: Map[String, String] = Map()
) {
  /** Builds the appropriate source with overridden config. */
  def buildSourceFunction: SourceFunction[Measurement] with HasFlinkConfig = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    sourceType
      .buildSourceFunction(sourceSubtype)
      .overrideConfig(config, configPrefix)
  }

  def buildFileInputFormat: (FileInputFormat[Measurement] with HasFlinkConfig, ParameterTool => FilePathFilter) = {
    val configPrefixNoSubtype = s"source.$sourceType"
    val configPrefix = sourceSubtype.map(s => s"$configPrefixNoSubtype.$s")
      .getOrElse(configPrefixNoSubtype)

    val (format, filter) = sourceType.buildFileInputFormat(sourceSubtype)

    format.setNestedFileEnumeration(true)

    (
      format.overrideConfig(config, configPrefix),
      filter
    )
  }
}
