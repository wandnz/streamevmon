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

package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.distdiff._
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

object DistDiffRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    System.setProperty("source.influx.subscriptionName", "DistDiffDetector")

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP ICMP")
      .uid("distdiff-source")
      .filter(_.defaultValue.nonEmpty)
      .name("Has data?")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    lazy val window = source.countWindow(config.getInt("detector.distdiff.recentsCount") * 2, 1)

    val process = if (config.getBoolean("detector.distdiff.useFlinkTimeWindow")) {
      val detector = new WindowedDistDiffDetector[LatencyTSAmpICMP, GlobalWindow]
      window
        .process(detector)
        .name(detector.flinkName)
    }
    else {
      {
        val detector = new DistDiffDetector[LatencyTSAmpICMP]
        source
          .process(detector)
          .name(detector.flinkName)
        }
        .uid("distdiff-detector")
        .setParallelism(1)
    }
    process.print("distdiff-printer")

    env.execute("Latency TS AMP ICMP -> Dist Diff Detector")
  }
}
