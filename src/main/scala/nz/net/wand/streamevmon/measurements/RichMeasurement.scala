package nz.net.wand.streamevmon.measurements

/** Represents a particular measurement at a point in time, complete with its
  * associated metadata ([[nz.net.wand.streamevmon.measurements.amp.MeasurementMeta MeasurementMeta]]
  * in the case of AMP measurements).
  */
trait RichMeasurement extends Measurement {}
