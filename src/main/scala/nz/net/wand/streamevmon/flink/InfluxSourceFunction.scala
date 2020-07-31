package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements.InfluxMeasurement

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
  * These measurements arrive as basic Measurement types: ICMP, DNS, and so on.
  *
  * ==Configuration==
  *
  * If a custom configuration is desired, the overrideConfig function can be
  * called before any calls to run(). This will replace the configuration
  * obtained from Flink's global configuration storage.
  *
  * This custom configuration is also used to configure the
  * [[nz.net.wand.streamevmon.connectors.InfluxConnection InfluxConnection]]
  * and [[nz.net.wand.streamevmon.connectors.InfluxHistoryConnection InfluxHistoryConnection]]
  * used.
  *
  * @tparam T The type used to represent the data received.
  *
  * @see [[AmpMeasurementSourceFunction]]
  * @see [[AmpRichMeasurementSourceFunction]]
  */
abstract class InfluxSourceFunction[T <: InfluxMeasurement](
  datatype    : String = "amp",
  fetchHistory: Duration = Duration.ZERO
)
  extends RichSourceFunction[T]
          with GloballyStoppableFunction
          with HasFlinkConfig
          with Logging
          with CheckpointedFunction {

  lazy override val configKeyGroup: String = "influx"

  @volatile
  @transient protected[this] var isRunning = false

  protected[this] var listener: Option[ServerSocket] = Option.empty

  @transient protected[this] var influxConnection: Option[InfluxConnection] = None

  @transient protected[this] var influxHistory: Option[InfluxHistoryConnection] = None

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

  var maxLateness: Long = _

  /** Transforms a single line received from InfluxDB in Line Protocol format
    * into an object of type T.
    *
    * @param line The line received.
    *
    * @return The object representing the data received.
    */
  protected[this] def processLine(line: String): Option[T]

  /** Overriding this function allows implementing classes to perform additional
    * processing on historical data received in Measurement format.
    *
    * @return The measurement as the more specific type T, or None if conversion
    *         failed.
    */
  protected[this] def processHistoricalMeasurement(measurement: InfluxMeasurement): Option[T] = {
    Some(measurement.asInstanceOf[T])
  }

  /** Starts the source, setting up the listen server.
    *
    * @param ctx The SourceContext associated with the current execution.
    */
  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    // Set up config
    val params = configWithOverride(getRuntimeContext)
    influxConnection = Some(InfluxConnection(params, configKeyGroup, datatype))
    influxHistory = Some(InfluxHistoryConnection(params, configKeyGroup, datatype))
    maxLateness = params.getLong("flink.maxLateness")

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Listen for new data
    if (!startListener()) {
      logger.error(s"Failed to start listener.")
    }
    else {
      logger.debug("Started listener")

      // Get historical data - we do it now instead of before starting the listener
      // since there could be a small time that a measurement occurs in after the
      // history query is complete but before the listener starts.
      // We could also keep track of the measurements received and check if the
      // first couple of measurements that come through the listener are duplicates
      // because of the potential overlap we've created, but that's not a big deal.
      val now = Instant.now()
      val timeSinceLastMeasurement = Duration.between(lastMeasurementTime, now)
      val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
      logger.info(s"Fetching data since $lastMeasurementTime ($historyString ago)")
      val historicalData = influxHistory.get.getAllAmpData(lastMeasurementTime, now)
      historicalData.foreach { m =>
        processHistoricalMeasurement(m) match {
          case Some(value) => ctx.collectWithTimestamp(value, value.time.toEpochMilli)
          case None => logger.error(s"Historical entry failed to parse: $m")
        }
      }
      if (historicalData.nonEmpty) {
        lastMeasurementTime = historicalData.maxBy(_.time).time
        ctx.emitWatermark(new Watermark(lastMeasurementTime.minusSeconds(maxLateness).toEpochMilli))
      }

      listen(ctx)

      stopListener()
      logger.debug("Stopped listener")
    }

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
  protected[this] def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true

    while (isRunning && !shouldShutdown) {
      try {
        listener match {
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

            // This is hardcoded for HTTP. If we ever care to make UDP or HTTPS
            // work, this will need refactoring. Chances are we won't bother.
            Stream
              .continually {
                reader.readLine
              }
              .takeWhile(line => line != null)
              .dropWhile(line => !line.isEmpty)
              .drop(1)
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
            logger.warn("Listener unexpectedly died")
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

  protected[this] def startListener(): Boolean = {
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

  protected[this] def stopListener(): Unit =
    listener.foreach(l => influxConnection.foreach(_.stopSubscriptionListener(l)))

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
