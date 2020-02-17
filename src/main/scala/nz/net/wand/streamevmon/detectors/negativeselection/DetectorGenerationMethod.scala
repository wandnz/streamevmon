package nz.net.wand.streamevmon.detectors.negativeselection

case class DetectorGenerationMethod(
  redundancy: Boolean = true,
  spatialPreference: Boolean = true,
  featurePreference: Boolean = true,
  borderProportion: Double = 0.1
)
