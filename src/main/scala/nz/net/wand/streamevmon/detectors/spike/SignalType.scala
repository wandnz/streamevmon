package nz.net.wand.streamevmon.detectors.spike

/** Spike detector signal type. Use the `id` field to get a numeric value.
  *
  * A negative signal means that the associated measurement was lower than
  * expected. A positive signal means it was higher. No signal means it was
  * within the bounds of reason.
  */
object SignalType extends Enumeration {
  val Negative: Value = Value(-1)
  val NoSignal: Value = Value(0)
  val Positive: Value = Value(1)
}
