package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.measurements.traits.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.Duration

/** Produces [[nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement InfluxMeasurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  *      object for configuration details.
  */
class AmpMeasurementSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[InfluxMeasurement](
    "amp",
    fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: InfluxMeasurement): Option[InfluxMeasurement] = {
    Some(measurement)
  }

  override protected def processLine(line: String): Option[InfluxMeasurement] = {
    InfluxMeasurementFactory.createMeasurement(line)
  }

  override val flinkName: String = "AMP Measurement Source"
  override val flinkUid: String = "amp-measurement-source"
}
