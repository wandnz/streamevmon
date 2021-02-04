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
import nz.net.wand.streamevmon.detectors.changepoint._
import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction
import nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import java.time.Duration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala._

/** This is the main runner for the changepoint detector, which is
  * found in the [[nz.net.wand.streamevmon.detectors.changepoint]] package.
  *
  * @see [[nz.net.wand.streamevmon.detectors.changepoint the package description]] for details.
  * @see [[nz.net.wand.streamevmon.runners.detectors.ChangepointGraphs ChangepointGraphs]] for an alternative bulk runner.
  */
object ChangepointRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    System.setProperty("source.influx.subscriptionName", "ChangepointDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      .addSource(new AmpMeasurementSourceFunction)
      .name("Measurement Subscription")
      .uid("changepoint-measurement-sourcefunction")
      .filter(_.isInstanceOf[ICMP])
      .name("Is ICMP?")
      .uid("changepoint-filter-is-icmp")
      .filter(!_.isLossy)
      .name("Has data?")
      .uid("changepoint-filter-has-data")
      .keyBy(new MeasurementKeySelector[InfluxMeasurement])

    implicit val ti: TypeInformation[NormalDistribution[InfluxMeasurement]] = TypeInformation.of(classOf[NormalDistribution[InfluxMeasurement]])

    val detector = new ChangepointDetector
                         [InfluxMeasurement, NormalDistribution[InfluxMeasurement]](
      NormalDistribution(mean = 0)
    )

    val process = source
      .process(detector)
      .name(detector.flinkName)
      .uid("changepoint-processor")

    process.addSink(new InfluxSinkFunction)
      .name("Influx Sink")
      .uid("changepoint-influx-sink")

    env.execute("Measurement subscription -> Changepoint Detector")
  }
}
