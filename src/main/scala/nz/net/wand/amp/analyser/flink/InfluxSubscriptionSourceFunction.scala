package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.{Configuration, Logging}
import nz.net.wand.amp.analyser.connectors.InfluxConnection

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}
import java.time.Instant

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

abstract class InfluxSubscriptionSourceFunction[T]()
    extends SourceFunction[T]
    with Logging
    with Configuration {

  private[this] var isRunning = false
  private[this] var listener: Option[ServerSocket] = Option.empty

  val watermarkFrequency: Int = getConfigInt("watermarkFrequency").getOrElse(1)

  protected[this] def processLine(ctx: SourceFunction.SourceContext[T], line: String): Option[T]

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

  private[this] def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true
    while (isRunning) {
      try {
        listener match {
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

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

  var lastWatermark: Instant = Instant.EPOCH

  protected[this] def submitWatermark(ctx: SourceFunction.SourceContext[T], time: Instant): Unit = {
    if (java.time.Duration.between(lastWatermark, time).getSeconds > watermarkFrequency) {
      val newWatermark = new Watermark(time.toEpochMilli)
      ctx.emitWatermark(newWatermark)
      lastWatermark = time
    }
  }

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
