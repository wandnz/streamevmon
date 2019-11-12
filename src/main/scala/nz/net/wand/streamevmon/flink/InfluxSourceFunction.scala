package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements.Measurement

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}
import java.time.{Duration, Instant}
import java.util.{List => JavaList}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => ScalaDuration}

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
  * - flink.maxLateness: The number of seconds after receiving data that a
  * watermark should be emitted for the time at which the data was received.
  * After this watermark is emitted, any data which is received with a
  * timestamp that occurs before that watermark is considered late. Currently,
  * late data is dropped.
  * Default 1.
  *
  * If a custom configuration is desired, the overrideConfig function can be
  * called before any calls to [[run]]. This will replace the configuration
  * obtained from Flink's global configuration storage.
  *
  * This custom configuration is also used to configure the
  * [[nz.net.wand.streamevmon.connectors.InfluxConnection InfluxConnection]]
  * and [[nz.net.wand.streamevmon.connectors.InfluxHistoryConnection InfluxHistoryConnection]]
  * used.
  *
  * @tparam T The type used to represent the data received.
  *
  * @see [[MeasurementSourceFunction]]
  * @see [[RichMeasurementSourceFunction]]
  */
abstract class InfluxSourceFunction[T <: Measurement]
(fetchHistory: Duration = Duration.ZERO)
  extends GloballyStoppableFunction[T]
          with Logging
          with ListCheckpointed[Instant] {

  @volatile
  @transient private[this] var isRunning = false

  private[this] var listener: Option[ServerSocket] = Option.empty

  @transient private[this] var maxLateness: Int = 0

  @transient private[this] var influxConnection: Option[InfluxConnection] = None

  @transient private[this] var influxHistory: Option[InfluxHistoryConnection] = None

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory)

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
  protected[this] def processHistoricalMeasurement(measurement: Measurement): Option[T] = {
    Some(measurement.asInstanceOf[T])
  }

  private[this] var overrideParams: Option[ParameterTool] = None

  private[flink] def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
  }

  /** Starts the source, setting up the listen server.
    *
    * @param ctx The SourceContext associated with the current execution.
    */
  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    // Set up config
    if (overrideParams.isDefined) {
      influxConnection = Some(InfluxConnection(overrideParams.get))
      influxHistory = Some(InfluxHistoryConnection(overrideParams.get))
      maxLateness = overrideParams.get.getInt("flink.maxLateness")
    }
    else {
      val params: ParameterTool =
        getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
      influxConnection = Some(InfluxConnection(params))
      influxHistory = Some(InfluxHistoryConnection(params))
      maxLateness = params.getInt("flink.maxLateness")
    }

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
      val historicalData = influxHistory.get.getAllData(lastMeasurementTime, now)
      historicalData.foreach { m =>
        processHistoricalMeasurement(m) match {
          case Some(value) => ctx.collect(value)
          case None => logger.error(s"Historical entry failed to parse: $m")
        }
      }
      if (historicalData.nonEmpty) {
        lastMeasurementTime = historicalData.maxBy(_.time).time
      }

      val listenLoop = Future(listen(ctx))

      Await.result(listenLoop, ScalaDuration.Inf)

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
  private[this] def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true

    while (isRunning && !shouldShutdown) {
      try {
        listener match {
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

            // TODO: This is hardcoded for HTTP
            // If we can get UDP/HTTPS working, this will need looking at to make it compatible.
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
                  case None => logger.error(s"Entry failed to parse: $line")
                }
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

  /** Stops the source, allowing the listen loop to finish. */
  def stop(): Unit = cancel()

  private[this] def startListener(): Boolean = {
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

  private[this] def stopListener(): Unit =
    listener.foreach(l => influxConnection.foreach(_.stopSubscriptionListener(l)))

  override def snapshotState(checkpointId: Long, timestamp: Long): JavaList[Instant] = {
    Seq(lastMeasurementTime).asJava
  }

  override def restoreState(state: JavaList[Instant]): Unit = {
    lastMeasurementTime = state.get(0)
  }
}
