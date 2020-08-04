package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.Duration

/** Produces [[nz.net.wand.streamevmon.measurements.InfluxMeasurement InfluxMeasurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves
  * Libtrace-Bigdata flow measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors]] package
  *      object for configuration details.
  */
class BigDataSourceFunction(
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[InfluxMeasurement](
    fetchHistory = fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: InfluxMeasurement): Option[InfluxMeasurement] = {
    Some(measurement)
  }

  override protected def processLine(line: String): Option[InfluxMeasurement] = {
    InfluxMeasurementFactory.createMeasurement(line)
  }

  override val flinkName: String = "Libtrace-Bigdata Measurement Subscription"
  override val flinkUid: String = "bigdata-measurement-source"
}
