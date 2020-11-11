package nz.net.wand.streamevmon.measurements

/** Represents a particular measurement at a point in time, complete with
  * metadata about the test schedule it originated from.
  *
  * For AMP measurements, the metadata can be separately stored in
  * [[nz.net.wand.streamevmon.measurements.PostgresMeasurementMeta MeasurementMeta]]
  */
trait RichMeasurement extends Measurement {}
