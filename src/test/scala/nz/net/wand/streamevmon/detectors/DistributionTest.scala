package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.detectors.changepoint.NormalDistribution

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.WordSpec

import scala.language.implicitConversions

class DistributionTest extends WordSpec {

  implicit val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(0.000000000000001)

  "NormalDistribution" should {
    "generate the correct values" ignore {
      val initial = NormalDistribution[(Double, Double)](
        item = (0.0, 0.0),
        mapFunction = (x: (Double, Double)) => x._1
      )

      assert(initial.mean == 0.0)
      //assert(initial.variance == 0.0)

      // When only a single datapoint is present, there's only one possible
      // expected value: the mean.
      //assert(initial.pdf(0) == 1.0)
      //assert(initial.pdf(1) == 0.0)

      // I ran some maths externally to get these numbers. They look pretty magical.
      var current = initial
      val numbersToAdd = Seq(1.0, 0.5, -1.0, 2.0, -3.0, 4.0, -5.0)
      val expectedMean = Seq(1.0 / 2, 1.0 / 2, 1.0 / 8, 1.0 / 2, -1.0 / 12, 1.0 / 2, -3.0 / 16)
      val expectedVar = Seq(0.5, 0.25, 35.0 / 48, 5.0 / 4, 73.0 / 24, 59.0 / 12, 7.995535714285714)
      val expectedPdf = Seq(0.43939128946772243, 0.48394144903828673, 0.2763707386921524,
        0.3228684517430724, 0.1886119190948115, 0.17540149791660137, 0.12917797378077397)

      for ((i, mean, variance, pdf) <- (numbersToAdd zip expectedMean zip expectedVar zip expectedPdf)
             .map {
               case (((a, b), c), d) => (a, b, c, d)
             }) {
        current = current.withPoint((i, i))

        assert(current.mean === mean)
        assert(current.variance === variance)
        assert(current.pdf(1) === pdf)
      }
    }
  }
}
