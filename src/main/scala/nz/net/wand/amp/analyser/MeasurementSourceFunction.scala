package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements.{Measurement, MeasurementFactory}

import org.apache.flink.streaming.api.functions.source.SourceFunction

class MeasurementSourceFunction() extends InfluxSubscriptionSourceFunction[Measurement] {

  override protected def processLine(ctx: SourceFunction.SourceContext[Measurement],
                                     line: String): Unit = {
    if (line != null) {
      val result = MeasurementFactory.createMeasurement(line)
      result match {
        case Some(x) => ctx.collect(x)
        case None    =>
      }
    }
  }
}
