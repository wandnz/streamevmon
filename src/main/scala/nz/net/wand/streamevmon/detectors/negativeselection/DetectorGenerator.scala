package nz.net.wand.streamevmon.detectors.negativeselection

import java.util.concurrent.ThreadLocalRandom

case class DetectorGenerator(
  dimensions: Int,
  dimensionRanges : Seq[(Double, Double)],
  generationMethod: DetectorGenerationMethod = DetectorGenerationMethod()
) {

  def generate(): Detector = {
    Detector(
      dimensions,
      dimensionRanges.map(range => ThreadLocalRandom.current().nextDouble(range._1, range._2)),
      10
    )
  }

  def generateN(amount: Int): Seq[Detector] = {
    Range(0, amount).map(_ => generate())
  }
}
