package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.influx.InfluxHistoryConnection
import nz.net.wand.streamevmon.measurements.InfluxMeasurement

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.streaming.api.functions.source.SourceFunction

abstract class PollingInfluxSourceFunction[T <: InfluxMeasurement](
  configPrefix   : String = "influx",
  datatype       : String = "amp",
  fetchHistory   : Duration = Duration.ZERO,
  timeOffset     : Duration = Duration.ZERO,
  refreshInterval: Duration = Duration.ofMillis(500)
)
  extends InfluxSourceFunction[T] {

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

  override protected[this] def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true

    while (isRunning && !shouldShutdown) {
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
