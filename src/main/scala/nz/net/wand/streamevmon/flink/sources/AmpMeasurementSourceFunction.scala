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

package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.Duration

/** Produces [[nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement InfluxMeasurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  *      object for configuration details.
  */
class AmpMeasurementSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxAmpSourceFunction[InfluxMeasurement](
    "amp",
    fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: InfluxMeasurement): Option[InfluxMeasurement] = {
    Some(measurement)
  }

  override protected def processLine(line: String): Option[InfluxMeasurement] = {
    InfluxMeasurementFactory.createMeasurement(line)
  }

  override val flinkName: String = "AMP Measurement Source"
  override val flinkUid: String = "amp-measurement-source"
}
