package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.connectors.postgres.PostgresConnection
import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory, RichInfluxMeasurement}

import java.time.Duration

import org.apache.flink.configuration.Configuration

/** Produces [[nz.net.wand.streamevmon.measurements.traits.RichInfluxMeasurement RichInfluxMeasurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  *      object for configuration details.
  */
class AmpRichMeasurementSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[RichInfluxMeasurement](
    "amp",
    fetchHistory
  ) {

  @transient private var pgConnection: PostgresConnection = _

  override def open(parameters: Configuration): Unit = {
    val globalParams = configWithOverride(getRuntimeContext)
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

  override val flinkName: String = "AMP Rich Measurement Source"
  override val flinkUid: String = "amp-rich-measurement-source"
}
