package nz.net.wand.streamevmon.measurements

trait HasDefault {
  /** @return The value that is most likely to be useful to detectors. For
    *         example, an ICMP measurement might return its latency. This allows
    *         us to easily get a useful value from a flow of several measurement
    *         types without needing to know what each item is.
    */
  var defaultValue: Option[Double]
}
