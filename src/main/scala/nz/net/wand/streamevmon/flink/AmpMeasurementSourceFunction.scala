package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}

import java.time.Duration

/** Receives [[nz.net.wand.streamevmon.measurements.Measurement Measurement]]
  * values from InfluxDB in a streaming fashion. This source retrieves AMP
  * measurements.
  *
  * @see [[nz.net.wand.streamevmon.connectors.InfluxConnection InfluxConnection]]
  *      and [[nz.net.wand.streamevmon.connectors.InfluxHistoryConnection InfluxHistoryConnection]]
  *      for configuration details.
  */
class AmpMeasurementSourceFunction(
  configPrefix: String = "influx.dataSource",
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[Measurement](
    configPrefix,
    "amp",
    fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: Measurement): Option[Measurement] = {
    Some(measurement)
  }

  override protected def processLine(line: String): Option[Measurement] = {
    MeasurementFactory.createMeasurement(line)
  }
}
