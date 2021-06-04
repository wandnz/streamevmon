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

import nz.net.wand.streamevmon.connectors.influx.{Amp2InfluxHistoryConnection, InfluxConnection}
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.amp2.Amp2Measurement
import nz.net.wand.streamevmon.Logging

import java.net.{ServerSocket, SocketTimeoutException}
import java.time.{Duration, Instant}

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.watermark.Watermark

import scala.io.Source

class Amp2SourceFunction(
  fetchHistory: Duration = Duration.ZERO
) extends RichSourceFunction[Amp2Measurement]
          with HasFlinkConfig
          with CheckpointedFunction
          with Logging {

  override val flinkName: String = "amp2 Source"
  override val flinkUid: String = "amp2-source"
  override val configKeyGroup: String = "influx"

  @volatile @transient protected var isRunning = false

  @transient protected var listener: Option[ServerSocket] = None

  @transient protected var influxConnection: Option[InfluxConnection] = None

  @transient protected var influxHistory: Option[Amp2InfluxHistoryConnection] = None

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

  protected var maxLateness: Long = _

  override def run(ctx: SourceFunction.SourceContext[Amp2Measurement]): Unit = {
    // Set up config
    val params = configWithOverride(getRuntimeContext)
    influxConnection = Some(InfluxConnection(params, configKeyGroup, "amp2"))
    influxHistory = Some(Amp2InfluxHistoryConnection(params, configKeyGroup, "amp2"))
    maxLateness = params.getLong("flink.maxLateness")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    collectHistoricalData(ctx)

    listen(ctx)

    stopListener()
    logger.debug(s"Stopped ${getClass.getSimpleName}")

    influxConnection.foreach(_.disconnect())
    influxConnection = None
    influxHistory = None
  }

  override def cancel(): Unit = {
    logger.info(s"Stopping ${getClass.getSimpleName}...")
    isRunning = false
  }

  protected def processHistoricalData(
    ctx: SourceFunction.SourceContext[Amp2Measurement],
    start: Instant,
    end: Instant
  ): Int = {
    influxHistory match {
      case None =>
        logger.warn("No influxHistory was created. Skipping historical data collection.")
        0
      case Some(history) =>
        logger.debug(s"Getting more history between $start and $end")
        val data = history.getAllAmp2Data(start, end)
        data.foreach { meas =>
          // lastMeasurementTime could be inaccurate if this method is interrupted.
          // It should have the value of the most recent time observed in the result set.
          if (lastMeasurementTime.isBefore(meas.time)) {
            lastMeasurementTime = meas.time
          }
          ctx.collectWithTimestamp(meas, meas.time.toEpochMilli)
        }
        ctx.emitWatermark(new Watermark(lastMeasurementTime.minusSeconds(maxLateness).toEpochMilli))
        ctx.markAsTemporarilyIdle()
        data.size
    }
  }

  protected def collectHistoricalData(ctx: SourceFunction.SourceContext[Amp2Measurement]): Unit = {
    var continue = true
    while (continue) {
      val numItems = processHistoricalData(ctx, lastMeasurementTime, Instant.now())
      logger.debug(s"Got $numItems new items")
      if (numItems == 0) {
        continue = false
      }
    }
  }

  protected def listen(ctx: SourceFunction.SourceContext[Amp2Measurement]): Unit = {
    logger.info("Listening for subscribed events...")

    if (!startListener()) {
      logger.error("Couldn't start listener!")
      return
    }

    isRunning = true

    while (isRunning) {
      try {
        listener match {
          case None =>
            logger.error("Listener unexpectedly died. Exiting.")
            isRunning = false
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)

            Source
              .fromInputStream(sock.getInputStream)
              .getLines()
              // Drop the HTTP header - some number of non-empty lines, followed by one empty line
              .dropWhile(_.nonEmpty)
              .drop(1)
              .foreach { line =>
                Amp2Measurement.createFromLineProtocol(line) match {
                  case None => logger.warn(s"Entry failed to parse: $line")
                  case Some(value) =>
                    lastMeasurementTime = value.time
                    ctx.collectWithTimestamp(value, value.time.toEpochMilli)
                    ctx.emitWatermark(new Watermark(value.time.minusSeconds(maxLateness).toEpochMilli))
                }
              }

            ctx.markAsTemporarilyIdle()
        }
      }
      catch {
        case _: SocketTimeoutException => // ignore, we want this to happen so we can loop on isRunning
      }
    }

    logger.info("Stopped listening for subscribed events.")
  }

  protected def startListener(): Boolean = {
    influxConnection match {
      case Some(c) =>
        listener = c.getSubscriptionListener
        listener match {
          case Some(_) => true
          case None => false
        }
      case None => false
    }
  }

  protected def stopListener(): Unit = {
    listener.foreach(l => influxConnection match {
      case Some(value) => value.stopSubscriptionListener(l)
      case None => logger.error("Couldn't drop subscription! influxConnection doesn't exist.")
    })
  }

  // We only put the last measurement time in our checkpoint state. The rest
  // is transient and should get reconstructed at next startup.
  private var checkpointState: ListState[Instant] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.add(lastMeasurementTime)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[Instant]("lastMeasurementTime", classOf[Instant]))

    if (context.isRestored) {
      lastMeasurementTime = checkpointState.get().iterator().next()
    }
  }
}
