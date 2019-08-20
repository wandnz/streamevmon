package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.{Configuration, Logging}
import nz.net.wand.amp.analyser.connectors.InfluxConnection

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}
import java.time.Instant
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark

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
  * Any implementations should call [[submitWatermark]] with the event time
  * of each point of data they receive. They should use ctx.collectWithTimestamp
  * to submit the data itself.
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
  * @tparam T The type used to represent the data received.
  *
  * @see [[MeasurementSubscriptionSourceFunction]]
  * @see [[RichMeasurementSubscriptionSourceFunction]]
  */
abstract class InfluxSubscriptionSourceFunction[T]
  extends StoppableSourceFunction[T]
          with Logging
          with Configuration {

  @volatile
  @transient private[this] var isRunning = false

  private[this] var listener: Option[ServerSocket] = Option.empty

  configPrefix = "flink"
  @transient private[this] val maxLateness: Int = getConfigInt("maxLateness").getOrElse(1)

  // We reuse the class loader for our ActorSystem to get reliable behaviour
  // during tests in the sbt shell.
  @transient private[this] lazy val ourClassLoader: ClassLoader = this.getClass.getClassLoader
  @transient private[this] lazy val actorSystem =
    akka.actor.ActorSystem("watermarkEmitter", classLoader = Some(ourClassLoader))

  /** Transforms a single line received from InfluxDB in Line Protocol format
    * into an object of type T.
    *
    * @param ctx  The SourceContext associated with the current execution.
    * @param line The line received.
    *
    * @return The object representing the data received.
    */
  protected[this] def processLine(ctx: SourceFunction.SourceContext[T], line: String): Option[T]

  /** Starts the source, setting up the listen server.
    *
    * @param ctx The SourceContext associated with the current execution.
    */
  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    if (!startListener()) {
      logger.error(s"Failed to start listener.")
    }
    else {
      logger.info("Started listener")

      val subscribeFuture = Future(listen(ctx))

      Await.result(subscribeFuture, Duration.Inf)

      stopListener()
      logger.info("Stopped listener")
      InfluxConnection.disconnect()
    }
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

    logger.info("No longer listening")
  }

  /** Submits a watermark to Flink after the configured amount of time.
    *
    * @param ctx  The SourceContext associated with the current execution.
    * @param time The time to submit a watermark for.
    */
  protected[this] def submitWatermark(ctx: SourceFunction.SourceContext[T], time: Instant): Unit = {
    actorSystem.scheduler
      .scheduleOnce(maxLateness.seconds) {
        ctx.emitWatermark(new Watermark(time.toEpochMilli + TimeUnit.SECONDS.toMillis(maxLateness)))
      }
  }

  /** Stops the source, allowing the listen loop to finish.
    */
  override def cancel(): Unit = {
    logger.info("Stopping listener...")
    isRunning = false
  }

  private[this] def startListener(): Boolean = {
    listener = InfluxConnection.getSubscriptionListener
    listener match {
      case Some(_) => true
      case None    => false
    }
  }

  private[this] def stopListener(): Unit =
    listener.foreach(l => InfluxConnection.stopSubscriptionListener(l))
}
