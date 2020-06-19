package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.PostgresConnection
import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory, RichInfluxMeasurement}

import java.time.Duration

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration

/** Receives [[nz.net.wand.streamevmon.measurements.RichMeasurement RichMeasurement]]
  * values from InfluxDB in a streaming fashion.This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.InfluxConnection InfluxConnection]]
  *      and [[nz.net.wand.streamevmon.connectors.InfluxHistoryConnection InfluxHistoryConnection]]
  *      for configuration details.
  */
class AmpRichMeasurementSourceFunction(
  configPrefix: String = "influx.dataSource",
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[RichInfluxMeasurement](
    configPrefix,
    "amp",
    fetchHistory
  ) {

  private[this] var pgConnection: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams =
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    pgConnection = PostgresConnection(globalParams)
  }

  override protected def processHistoricalMeasurement(
    measurement: InfluxMeasurement
  ): Option[RichInfluxMeasurement] = {
    InfluxMeasurementFactory.enrichMeasurement(pgConnection, measurement)
  }

  override protected def processLine(line: String): Option[RichInfluxMeasurement] = {
    InfluxMeasurementFactory.createRichMeasurement(pgConnection, line)
  }
}
