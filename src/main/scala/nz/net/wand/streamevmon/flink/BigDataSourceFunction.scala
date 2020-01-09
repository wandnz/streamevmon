package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.{Measurement, MeasurementFactory}

import java.time.Duration

class BigDataSourceFunction(
  configPrefix: String = "influx.dataSource",
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[Measurement](
    configPrefix = configPrefix,
    datatype = "bigdata",
    fetchHistory = fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: Measurement): Option[Measurement] = {
    Some(measurement)
  }

  override protected def processLine(line: String): Option[Measurement] = {
    MeasurementFactory.createMeasurement(line)
  }
}
