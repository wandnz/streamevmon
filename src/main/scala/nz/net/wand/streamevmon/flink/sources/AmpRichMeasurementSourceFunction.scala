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

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory, RichInfluxMeasurement}

import java.time.Duration

import org.apache.flink.configuration.Configuration

/** Produces [[nz.net.wand.streamevmon.measurements.traits.RichInfluxMeasurement RichInfluxMeasurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  *      object for configuration details.
  */
class AmpRichMeasurementSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxAmpSourceFunction[RichInfluxMeasurement](
    "amp",
    fetchHistory
  ) {

  @transient private var pgConnection: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams = configWithOverride(getRuntimeContext)
    pgConnection = PostgresConnection(globalParams)
  }

  override protected def processHistoricalMeasurement(
    measurement: InfluxMeasurement
  ): Option[RichInfluxMeasurement] = {
    InfluxMeasurementFactory.enrichMeasurement(pgConnection, measurement)
  }

  override protected def processLine(line: String): Option[RichInfluxMeasurement] = {
    InfluxMeasurementFactory.createRichMeasurement(pgConnection, line)
  }

  override val flinkName: String = "AMP Rich Measurement Source"
  override val flinkUid: String = "amp-rich-measurement-source"
}
