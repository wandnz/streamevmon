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
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import org.apache.flink.streaming.api.scala._

/** Main runner for mode change detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.mode]] package.
  */
object ModeRunner {
  def main(args: Array[String]): Unit = {
    val filename = "data/latency-ts-i/ampicmp/waikato-xero-ipv4.series"
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    System.setProperty("source.influx.subscriptionName", "ModeDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, filename)
      .setParallelism(1)
      .name("Latency TS AMP Input")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    val detector = new ModeDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.flinkName)
      .uid(detector.flinkUid)

    process.print(s"Mode Event ($filename)")

    env.execute("Measurement subscription -> Mode Detector")
  }
}
