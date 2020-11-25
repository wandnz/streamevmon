package nz.net.wand.streamevmon.measurements.traits

/** A RichInfluxMeasurement is still an InfluxMeasurement, but it's also a
  * RichMeasurement. It should have all the traits associated with both.
  */
trait RichInfluxMeasurement
  extends InfluxMeasurement
          with RichMeasurement {}
