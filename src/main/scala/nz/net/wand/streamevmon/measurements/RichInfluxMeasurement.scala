package nz.net.wand.streamevmon.measurements

trait RichInfluxMeasurement
  extends InfluxMeasurement
          with RichMeasurement
          with CsvOutputable
          with HasDefault {}
