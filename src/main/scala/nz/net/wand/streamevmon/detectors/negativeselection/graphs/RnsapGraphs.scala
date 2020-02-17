package nz.net.wand.streamevmon.detectors.negativeselection.graphs

import nz.net.wand.streamevmon.detectors.negativeselection.{Detector, DetectorGenerationMethod, DetectorGenerator}

trait RnsapGraphs extends Serializable {
  def createGraph(
    detectors: Iterable[Detector],
    generator: DetectorGenerator,
    selfData: Iterable[Iterable[Double]] = Seq(Seq(), Seq()),
    nonselfData: Iterable[Iterable[Double]] = Seq(Seq(), Seq()),
    dimensionRanges: Iterable[(Double, Double)] = Seq((0, 1), (0, 1)),
    dimensionNames: Iterable[String] = Seq("X", "Y"),
    generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
  ): Unit
}
