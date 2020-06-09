package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.esmond.schema.TimeSeriesEntry
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.EsmondConnectionForeground

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

// TODO
// - Allow usage of summary serieses. Will need to move calls involving `esmond`
//   to a new function which switches on the option.
// - Figure out what doSomethingWithMeasurement will do. We might just want to
//   make a new measurements.Measurement subclass wrapping TimeSeriesEntry,
//   or we might even be able to make that a subclass itself.
// - Improve metadataKey and eventType specification. Eventually we probably
//   want a way to auto-discover all streams and query them all individually.
//   We'll want to increase refreshInterval quite a bit for that, and probably
//   stagger calls proportionally so that we don't DOS the host every time we
//   refresh.
// - We definitely want a way to filter out eventTypes we don't care about when
//   we do that. This could be done either via simple post-request filtering
//   here, or by allowing the user of this class to provide filtering query
//   strings for the discovery /archive/ call.

// We follow the same model here as we do in [[PollingInfluxSourceFunction]].
// This means there's a few quirks that are not strictly necessary, like the
// separate listen() function.
class PollingEsmondSourceFunction(
  configPrefix: String = "esmond.dataSource",
  fetchHistory: Duration = Duration.ofMinutes(10),
  timeOffset: Duration = Duration.ofHours(1),
  refreshInterval: Duration = Duration.ofMinutes(1),
  metadataKey: String = "083378c49e254f8fb768233a392bb400",
  eventType: String = "packet-count-sent"
)
  extends RichSourceFunction[TimeSeriesEntry]
          with GloballyStoppableFunction
          with Logging {
  @transient protected[this] var isRunning = false

  @transient protected[this] var esmond: Option[EsmondConnectionForeground] = None

  protected[this] var overrideParams: Option[ParameterTool] = None

  var lastMeasurementTime: Instant = Instant.now().minus(fetchHistory).minus(timeOffset)

  def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
  }

  def doSomethingWithMeasurement(timeSeriesEntry: TimeSeriesEntry): Unit = {
    logger.debug(timeSeriesEntry.toString)
  }

  override def run(ctx: SourceFunction.SourceContext[TimeSeriesEntry]): Unit = {
    // Set up config
    val params: ParameterTool = overrideParams.getOrElse(
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    )
    esmond = Some(EsmondConnectionForeground(params.get(s"$configPrefix.serverName")))

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Get historical data
    val now = Instant.now().minus(timeOffset)
    val timeSinceLastMeasurement = Duration.between(lastMeasurementTime, now)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since ${lastMeasurementTime.plus(timeOffset)} ($historyString ago)")

    val historicalData = esmond.get.getTimeSeriesEntries(
      metadataKey,
      eventType,
      timeStart = Some(lastMeasurementTime.getEpochSecond),
      timeEnd = Some(now.getEpochSecond)
    )
    historicalData.map(_.foreach(doSomethingWithMeasurement))
    if (historicalData.isSuccess && historicalData.get.nonEmpty) {
      lastMeasurementTime = Instant.ofEpochSecond(historicalData.get.maxBy(_.timestamp).timestamp).plusSeconds(1)
    }

    listen(ctx)
  }

  protected[this] def listen(ctx: SourceFunction.SourceContext[TimeSeriesEntry]): Unit = {
    {
      val durationString = DurationFormatUtils.formatDuration(refreshInterval.toMillis, "H:mm:ss")
      logger.info(s"Polling for events every $durationString...")
    }

    isRunning = true

    while (isRunning && !shouldShutdown) {
      Thread.sleep(refreshInterval.toMillis)

      val data = esmond.get.getTimeSeriesEntries(
        metadataKey,
        eventType,
        timeStart = Some(lastMeasurementTime.getEpochSecond),
        timeEnd = Some(Instant.now().minus(timeOffset).getEpochSecond)
      )

      data.map(_.foreach(doSomethingWithMeasurement))
      if (data.isSuccess && data.get.nonEmpty) {
        lastMeasurementTime = Instant.ofEpochSecond(data.get.maxBy(_.timestamp).timestamp).plusSeconds(1)
      }
    }
  }

  override def cancel(): Unit = {
    logger.info("Stopping esmond polling...")
    isRunning = false
  }
}
