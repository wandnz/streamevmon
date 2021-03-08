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

package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction
import nz.net.wand.streamevmon.runners.unified.schema.{SinkInstance, SinkType}
import nz.net.wand.streamevmon.test.TestBase

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction

import scala.collection.JavaConverters._

class SinkBuildTest extends TestBase {
  "Sources should build correctly" when {
    "built by a SinkInstance" when {
      "sink type is Influx" in {
        val srcInstance = SinkInstance(
          SinkType.Influx,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.build

        built shouldBe an[InfluxSinkFunction]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"sink.${built.configKeyGroup}.extraKey" -> "true")
      }

      "sink type is Print" in {
        val srcInstance = SinkInstance(
          SinkType.Print,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.build

        built shouldBe a[PrintSinkFunction[_]]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"sink.${built.configKeyGroup}.extraKey" -> "true")
      }
    }
  }
}
