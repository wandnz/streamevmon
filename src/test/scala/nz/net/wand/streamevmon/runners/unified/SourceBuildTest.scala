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

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.flink.sources._
import nz.net.wand.streamevmon.runners.unified.schema.{SourceInstance, SourceSubtype, SourceType}

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

class SourceBuildTest extends TestBase {
  "Sources should build correctly" when {
    "built by a SourceInstance" when {
      "source type is Influx/Amp" in {
        val srcInstance = SourceInstance(
          SourceType.Influx,
          Some(SourceSubtype.Amp),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe an[AmpMeasurementSourceFunction]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.amp.extraKey" -> "true")
      }

      "source type is Influx/Bigdata" in {
        val srcInstance = SourceInstance(
          SourceType.Influx,
          Some(SourceSubtype.Bigdata),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe a[BigDataSourceFunction]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.bigdata.extraKey" -> "true")
      }

      "source type is Esmond" in {
        val srcInstance = SourceInstance(
          SourceType.Esmond,
          None,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe a[PollingEsmondSourceFunction[_, _]]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.extraKey" -> "true")
      }

      "source type is LatencyTS/Amp" in {
        val srcInstance = SourceInstance(
          SourceType.LatencyTS,
          Some(SourceSubtype.LatencyTSAmp),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildFileInputFormat._1

        built shouldBe a[LatencyTSAmpFileInputFormat]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildSourceFunction
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.extraKey" -> "true")
      }

      "source type is LatencyTS/Smokeping" in {
        val srcInstance = SourceInstance(
          SourceType.LatencyTS,
          Some(SourceSubtype.LatencyTSSmokeping),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildFileInputFormat._1

        built shouldBe a[LatencyTSSmokepingFileInputFormat]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildSourceFunction
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.extraKey" -> "true")
      }

      "source type is NAB" in {
        val srcInstance = SourceInstance(
          SourceType.NAB,
          None,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildFileInputFormat._1

        built shouldBe a[NabFileInputFormat]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildSourceFunction
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.extraKey" -> "true")
      }
    }
  }

  "Sources should fail to build" when {
    "built by a SourceInstance" when {
      "types are mismatched" in {
        val allInvalidCombinations = Seq(
          (SourceType.Influx, SourceSubtype.LatencyTSAmp),
          (SourceType.Influx, SourceSubtype.LatencyTSSmokeping),
          (SourceType.LatencyTS, SourceSubtype.Amp),
          (SourceType.LatencyTS, SourceSubtype.Bigdata),
        )

        allInvalidCombinations.foreach { case (t, st) =>
          withClue(s"When type is $t and subtype is $st,") {
            t match {
              case SourceType.Influx | SourceType.Esmond =>
                an[IllegalArgumentException] should be thrownBy SourceInstance(t, Some(st)).buildSourceFunction
              case SourceType.LatencyTS =>
                an[IllegalArgumentException] should be thrownBy SourceInstance(t, Some(st)).buildFileInputFormat
            }
          }
        }
      }
    }
  }
}
