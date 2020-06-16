package nz.net.wand.streamevmon.measurements

trait InfluxMeasurement
  extends Measurement
          with CsvOutputable
          with HasDefault {}
