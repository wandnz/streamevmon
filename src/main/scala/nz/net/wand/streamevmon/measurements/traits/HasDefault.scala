package nz.net.wand.streamevmon.measurements.traits

/** Contains an optional defaultValue field which can be used to provide
  * detectors the "most-likely-to-be-useful" value. For example, an ICMP
  * measurement might return its latency.
  *
  * If a Measurement provides the wrong default, it can be overwritten after
  * construction, since it's a var. A good way to do this is via a .map() call
  * on a `DataStream[Measurement with HasDefault]`.
  */
trait HasDefault {
  var defaultValue: Option[Double]
}
