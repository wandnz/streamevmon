package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.measurements.{MeasurementFactory, RichMeasurement}

import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Receives [[nz.net.wand.amp.analyser.measurements.RichMeasurement RichMeasurement]]
  * values from InfluxDB in a streaming fashion.
  */
class RichMeasurementSubscriptionSourceFunction() extends InfluxSubscriptionSourceFunction[RichMeasurement] {

  override protected def processLine(ctx: SourceFunction.SourceContext[RichMeasurement],
                                     line: String): Option[RichMeasurement] = {
    val result = MeasurementFactory.createRichMeasurement(line)
    result match {
      case Some(x) =>
        ctx.collectWithTimestamp(x, x.time.toEpochMilli)
        submitWatermark(ctx, x.time)
      case None => logger.error(s"Entry failed to parse: $line")
    }
    result
  }
}
