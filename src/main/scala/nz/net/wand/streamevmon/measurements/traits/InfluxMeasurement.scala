package nz.net.wand.streamevmon.measurements.traits

/** All measurements from InfluxDB have the traits included in this bundle.
  */
trait InfluxMeasurement
  extends Measurement
          with CsvOutputable
          with HasDefault {}