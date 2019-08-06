package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.measurements.{MeasurementFactory, RichMeasurement}

import org.apache.flink.streaming.api.functions.source.SourceFunction

class RichMeasurementSourceFunction() extends InfluxSubscriptionSourceFunction[RichMeasurement] {

  override protected def processLine(ctx: SourceFunction.SourceContext[RichMeasurement],
                                     line: String): Unit = {
    if (line != null) {
      val result = MeasurementFactory.createRichMeasurement(line)
      result match {
        case Some(x) => ctx.collectWithTimestamp(x, x.time.toEpochMilli)
        case None    => logger.error(s"Entry failed to parse: $line")
      }
    }
  }
}
