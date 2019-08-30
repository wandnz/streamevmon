package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.connectors.PostgresConnection
import nz.net.wand.amp.analyser.measurements.{MeasurementFactory, RichMeasurement}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** Receives [[nz.net.wand.amp.analyser.measurements.RichMeasurement RichMeasurement]]
  * values from InfluxDB in a streaming fashion.
  */
class RichMeasurementSubscriptionSourceFunction
  extends InfluxSubscriptionSourceFunction[RichMeasurement] {

  private[this] var pgConnection: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams = getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    pgConnection = PostgresConnection(globalParams)
  }

  override protected def processLine(ctx: SourceFunction.SourceContext[RichMeasurement],
                                     line: String): Option[RichMeasurement] = {
    val result = MeasurementFactory.createRichMeasurement(pgConnection, line)
    result match {
      case Some(x) =>
        ctx.collectWithTimestamp(x, x.time.toEpochMilli)
      case None => logger.error(s"Entry failed to parse: $line")
    }
    result
  }
}
