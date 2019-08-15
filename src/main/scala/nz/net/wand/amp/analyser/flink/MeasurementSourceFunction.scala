package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.measurements.{Measurement, MeasurementFactory}

import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Receives [[nz.net.wand.amp.analyser.measurements.Measurement Measurement]]
  * values from InfluxDB in a streaming fashion.
  */
class MeasurementSourceFunction extends InfluxSubscriptionSourceFunction[Measurement] {

  override protected def processLine(ctx: SourceFunction.SourceContext[Measurement],
                                     line: String): Option[Measurement] = {
    val result = MeasurementFactory.createMeasurement(line)
    result match {
      case Some(x) =>
        ctx.collectWithTimestamp(x, x.time.toEpochMilli)
        submitWatermark(ctx, x.time)
      case None => logger.error(s"Entry failed to parse: $line")
    }
    result
  }
}