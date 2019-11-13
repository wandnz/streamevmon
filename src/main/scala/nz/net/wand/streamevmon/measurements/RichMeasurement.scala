package nz.net.wand.streamevmon.measurements

/** Represents a particular measurement at a point in time, complete with its
  * associated [[MeasurementMeta metadata]].
  */
abstract class RichMeasurement extends Measurement {
  /** Flink TypeExtractor wants a field with a getter and setter, so here it is. */
  private[measurements] var absolutelyUnusedField = true
}
