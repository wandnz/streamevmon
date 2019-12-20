package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.PostgresConnection
import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory, RichMeasurement}

import java.time.Duration

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration

/** Receives [[nz.net.wand.streamevmon.measurements.RichMeasurement RichMeasurement]]
  * values from InfluxDB in a streaming fashion.
  */
class RichMeasurementSourceFunction(
  configPrefix: String = "influx.dataSource",
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[RichMeasurement](
    configPrefix,
    fetchHistory
  ) {

  private[this] var pgConnection: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    pgConnection = PostgresConnection(globalParams)
  }

  override protected def processHistoricalMeasurement(
      measurement: Measurement
  ): Option[RichMeasurement] = {
    MeasurementFactory.enrichMeasurement(pgConnection, measurement)
  }

  override protected def processLine(line: String): Option[RichMeasurement] = {
    MeasurementFactory.createRichMeasurement(pgConnection, line)
  }
}
