package nz.net.wand.streamevmon.detectors.negativeselection

// This is really just a hypersphere.
// The length of `centre` must equal `dimensions`.
case class Detector(
  dimensions: Int,
  centre    : Seq[Double],
  radius    : Double
) {}
