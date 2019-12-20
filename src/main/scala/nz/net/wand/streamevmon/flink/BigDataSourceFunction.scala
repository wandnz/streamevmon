package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Duration

class BigDataSourceFunction(
  configPrefix: String = "influx.dataSource.bigdata",
  fetchHistory  : Duration = Duration.ZERO
)
  extends InfluxSourceFunction[Measurement](
    configPrefix,
    fetchHistory
  ) {

  override protected def processHistoricalMeasurement(measurement: Measurement): Option[Measurement] = None

  override protected def processLine(line: String): Option[Measurement] = None
}
