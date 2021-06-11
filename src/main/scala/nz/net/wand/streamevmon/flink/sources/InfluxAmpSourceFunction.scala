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

import nz.net.wand.streamevmon.connectors.influx.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}
import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.watermark.Watermark

/** Retrieves new data from InfluxDB as a streaming source function.
  *
  * Implementations should implement processLine, which is called once for each
  * datapoint received. The line is in InfluxDB's
  * [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/ Line Protocol]]
  * format.
  *
  * They may also optionally implement processHistoricalMeasurement, which allows
  * for additional processing on historical measurements received at startup.
  * These measurements arrive as concrete InfluxMeasurement types: ICMP, DNS, and so on.
  *
  * ==Configuration==
  *
  * See [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  * object for configuration details. Any configuration given to `overrideConfig`
  * from [[HasFlinkConfig]] will also be passed to the Influx connectors.
  *
  * @see [[Amp2SourceFunction]]
  */
abstract class InfluxAmpSourceFunction[T <: InfluxMeasurement](
  datatype    : String = "amp",
  fetchHistory: Duration = Duration.ZERO
)
  extends RichSourceFunction[T]
          with HasFlinkConfig
          with CheckpointedFunction
          with Logging {

  override val configKeyGroup: String = "influx"

  @volatile
  @transient protected var isRunning = false

  @transient protected var listener: Option[ServerSocket] = None

  @transient protected var influxConnection: Option[InfluxConnection] = None

  @transient protected var influxHistory: Option[InfluxHistoryConnection] = None

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

  protected var maxLateness: Long = _

  /** Transforms a single line received from InfluxDB in Line Protocol format
    * into an object of type T.
    *
    * @param line The line received.
    *
    * @return The object representing the data received.
    */
  protected def processLine(line: String): Option[T]

  /** Overriding this function allows implementing classes to perform additional
    * processing on historical data received in Measurement format.
    *
    * @return The measurement as the more specific type T, or None if conversion
    *         failed.
    */
  protected def processHistoricalMeasurement(measurement: InfluxMeasurement): Option[T] = {
    Some(measurement.asInstanceOf[T])
  }

  /** Starts the source, setting up the listen server. */
  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    // Set up config
    val params = configWithOverride(getRuntimeContext)
    influxConnection = Some(InfluxConnection(params, configKeyGroup, datatype))
    influxHistory = Some(InfluxHistoryConnection(params, configKeyGroup, datatype))
    maxLateness = params.getLong("flink.maxLateness")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Get historical data - we do it now instead of before starting the listener
    // since there could be a small time that a measurement occurs in after the
    // history query is complete but before the listener starts.
    // We could also keep track of the measurements received and check if the
    // first couple of measurements that come through the listener are duplicates
    // because of the potential overlap we've created, but that's not a big deal.
    val startOfHistoryGettingTime = Instant.now()
    val timeSinceLastMeasurement = Duration.between(lastMeasurementTime, startOfHistoryGettingTime)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since $lastMeasurementTime ($historyString ago)")

    def processHistory(start: Instant, end: Instant): Int = {
      var count = 0
      influxHistory.get.getAllAmpData(start, end)
        .foreach { m =>
          try {
            processHistoricalMeasurement(m) match {
              case Some(value) =>
                lastMeasurementTime = value.time
                count += 1
                ctx.collectWithTimestamp(value, value.time.toEpochMilli)
              case None => logger.error(s"Historical entry failed to parse: $m")
            }
          }
          catch {
            case e: Throwable => println(e)
          }
        }
      ctx.emitWatermark(new Watermark(lastMeasurementTime.minusSeconds(maxLateness).toEpochMilli))
      count
    }

    var keepGettingHistory = true
    while (keepGettingHistory) {
      logger.debug(s"Getting more history at ${Instant.now()}")
      val historyItemsReceived = processHistory(lastMeasurementTime, Instant.now())
      logger.debug(s"Got $historyItemsReceived new items")
      if (historyItemsReceived == 0) {
        keepGettingHistory = false
      }
    }

    listen(ctx)

    stopListener()
    logger.debug("Stopped listener")

    // Tidy up
    influxConnection.foreach(_.disconnect())
    influxConnection = None
    influxHistory = None
  }

  /** Listens for new events. Calls [[processLine]] once per data line gathered.
    *
    * Currently only tested with an HTTP subscription. UDP is unlikely to work,
    * but HTTPS might.
    *
    * @param ctx The SourceContext associated with the current execution.
    */
  protected def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    if (!startListener()) {
      logger.warn("Couldn't start listener!")
      return
    }

    isRunning = true

    while (isRunning) {
      try {
        listener match {
          case Some(serverSock) =>
            // Get the socket to receive data on, and make a reader for it
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

            Iterator
              .continually {
                reader.readLine
              }
              // Stop reading when we reach the end of the transmission.
              .takeWhile(line => line != null)
              // Drop the HTTP header - some number of nonempty lines, followed
              // by one empty line.
              .dropWhile(line => line.nonEmpty)
              .drop(1)
              // Process the lines we care about.
              .foreach(line => {
                processLine(line) match {
                  case Some(value) =>
                    lastMeasurementTime = value.time
                    ctx.collectWithTimestamp(value, value.time.toEpochMilli)
                    ctx.emitWatermark(new Watermark(value.time.minusSeconds(maxLateness).toEpochMilli))
                  case None => logger.error(s"Entry failed to parse: $line")
                }
                ctx.markAsTemporarilyIdle()
              })

          case None =>
            logger.warn("Listener unexpectedly died. Exiting.")
            isRunning = false
        }
      } catch {
        case _: SocketTimeoutException =>
      }
    }

    logger.debug("No longer listening")
  }

  /** Stops the source, allowing the listen loop to finish. */
  override def cancel(): Unit = {
    logger.info("Stopping listener...")
    isRunning = false
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
