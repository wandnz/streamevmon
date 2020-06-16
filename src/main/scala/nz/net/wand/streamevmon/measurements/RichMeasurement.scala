package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.measurements.amp.MeasurementMeta

/** Represents a particular measurement at a point in time, complete with its
  * associated metadata ([[MeasurementMeta]] in the case of AMP measurements).
  */
trait RichMeasurement extends Measurement {}
