package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.time.Duration

class BigDataSourceFunction(
  configPrefix: String = "source.influx",
  fetchHistory: Duration = Duration.ZERO
)
  extends InfluxSourceFunction[InfluxMeasurement](
    configPrefix = configPrefix,
    datatype = "bigdata",
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
