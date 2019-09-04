package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.InfluxConnection
import nz.net.wand.streamevmon.Logging

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/** Retrieves new data from InfluxDB as a streaming source function.
  *
  * Implementations should implement processLine, which is called once for each
  * datapoint received. The line is in InfluxDB's
  * [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/ Line Protocol]]
  * format.
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
  * used.
  *
  * @tparam T The type used to represent the data received.
  *
  * @see [[MeasurementSubscriptionSourceFunction]]
  * @see [[RichMeasurementSubscriptionSourceFunction]]
  */
abstract class InfluxSubscriptionSourceFunction[T]
  extends GloballyStoppableFunction[T]
          with Logging {

  @volatile
  @transient private[this] var isRunning = false

  private[this] var listener: Option[ServerSocket] = Option.empty

  @transient private[this] var maxLateness: Int = 0

  @transient private[this] var influxConnection: Option[InfluxConnection] = None

  /** Transforms a single line received from InfluxDB in Line Protocol format
    * into an object of type T.
    *
    * @param ctx  The SourceContext associated with the current execution.
    * @param line The line received.
    *
    * @return The object representing the data received.
    */
  protected[this] def processLine(ctx: SourceFunction.SourceContext[T], line: String): Option[T]

  private[this] var overrideParams: Option[ParameterTool] = None

  private[flink] def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
  }

  /** Starts the source, setting up the listen server.
    *
    * @param ctx The SourceContext associated with the current execution.
    */
  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    if (overrideParams.isDefined) {
      influxConnection = Some(InfluxConnection(overrideParams.get))
      maxLateness = overrideParams.get.getInt("flink.maxLateness")
    }
    else {
      val params: ParameterTool =
        getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
      influxConnection = Some(InfluxConnection(params))
      maxLateness = params.getInt("flink.maxLateness")
    }

    if (!startListener()) {
      logger.error(s"Failed to start listener.")
    }
    else {
      logger.debug("Started listener")

      val subscribeFuture = Future(listen(ctx))

      Await.result(subscribeFuture, Duration.Inf)

      stopListener()
      logger.debug("Stopped listener")
    }

    influxConnection.foreach(_.disconnect())
    influxConnection = None
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
              .foreach(line => processLine(ctx, line))

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
}
