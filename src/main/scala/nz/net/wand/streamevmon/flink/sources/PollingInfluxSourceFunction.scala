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

import nz.net.wand.streamevmon.connectors.influx.InfluxHistoryConnection
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Gets data from InfluxDB in a polling fashion. Currently in a proof of
  * concept stage, and shouldn't be used over the other implementations of
  * InfluxSourceFunction.
  */
abstract class PollingInfluxSourceFunction[T <: InfluxMeasurement](
  configPrefix   : String = "influx",
  datatype       : String = "amp",
  fetchHistory   : Duration = Duration.ZERO,
  timeOffset     : Duration = Duration.ZERO,
  refreshInterval: Duration = Duration.ofMillis(500)
)
  extends InfluxAmpSourceFunction[T] {

  lastMeasurementTime = Instant.now().minus(fetchHistory).minus(timeOffset)

  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    // Set up config
    val params = configWithOverride(getRuntimeContext)
    influxHistory = Some(InfluxHistoryConnection(params, configPrefix, datatype))

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    val now = Instant.now().minus(timeOffset)
    val timeSinceLastMeasurement = Duration.between(lastMeasurementTime, now)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since ${lastMeasurementTime.plus(timeOffset)} ($historyString ago)")
    val historicalData = influxHistory.get.getAllAmpData(lastMeasurementTime, now)
    historicalData.foreach { m =>
      processHistoricalMeasurement(m) match {
        case Some(value) => ctx.collect(value)
        case None => logger.error(s"Historical entry failed to parse: $m")
      }
    }
    if (historicalData.nonEmpty) {
      lastMeasurementTime = historicalData.maxBy(_.time).time
    }

    listen(ctx)
  }

  override protected def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true

    while (isRunning) {
      Thread.sleep(refreshInterval.toMillis)
      val data = influxHistory.get.getAllAmpData(lastMeasurementTime, Instant.now().minus(timeOffset))
      data.foreach { m =>
        processHistoricalMeasurement(m) match {
          case Some(value) => ctx.collect(value)
          case None => logger.error(s"Entry failed to parse: $m")
        }
      }
      if (data.nonEmpty) {
        lastMeasurementTime = data.maxBy(_.time).time
      }
    }
  }
}
