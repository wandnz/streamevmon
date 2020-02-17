package nz.net.wand.streamevmon.detectors.negativeselection.graphs

import nz.net.wand.streamevmon.detectors.negativeselection.{Detector, DetectorGenerationMethod, DetectorGenerator}

class DummyGraphs extends RnsapGraphs {
  override def createGraph(
    detectors: Iterable[Detector],
    generator: DetectorGenerator,
    selfData : Iterable[Iterable[Double]],
    nonselfData: Iterable[Iterable[Double]],
    dimensionRanges: Iterable[(Double, Double)],
    dimensionNames: Iterable[String],
    generationMethod: DetectorGenerationMethod
  ): Unit = {}
}
