package nz.net.wand.streamevmon.detectors.changepoint

import org.scalatest.WordSpec

import scala.language.implicitConversions

class DistributionTest extends WordSpec {

  import NormalDistribution._

  "NormalDistribution" should {
    "generate the correct values" in {
      val initial = NormalDistribution[Double](
        mean = 0.0,
        n = 0,
        variance = 1E8,
        mapFunction = (x: Double) => x
      )

      assert(initial.mean === 0.0)
      assert(initial.variance === 1E8)

      // I ran some maths externally to get the mean and PDF numbers. The variances
      // are just taken from the actual results, which isn't super useful, but
      // ensuring that they're pinned means the PDF will match up against what
      // I calculated.
      // They look pretty magical, but they do represent the expected parameters
      // for the given progression of inputs.
      // The normal distribution is a bit wacky in that it fakes how many elements
      // are used in the calculation of the first mean.
      assert(initial.pdf(0) === 0.00003989422804014325)
      assert(initial.pdf(1) === 0.0000398942278406721)

      var current = initial
      val numbersToAdd = Seq(1.0, 0.5, -1.0, 2.0, -3.0, 4.0, -5.0)
      val expectedMean = Seq(1.0, 2.5 / 3, 0.375, 0.7, 1.0 / 12, 9.0 / 14, -1.0 / 16)
      val expectedVar = Seq(5E7, 3.333333338888889E7, 2.5000000671875E7,
        2.000000096E7, 1.6666669368055558E7, 1.4285718479591837E7, 1.2500007152343752E7)
      val expectedPdf = Seq(0.00005641895835477566, 0.00006909882980789, 0.00007978845438478,
        0.00008920620346597, 0.00009772049199778, 0.00010555019017665, 0.00011283787933200)

      var i = 0
      for ((r, mean, variance, pdf) <- (numbersToAdd zip expectedMean zip expectedVar zip expectedPdf)
             .map {
               case (((a, b), c), d) => (a, b, c, d)
             }) {
        i += 1
        current = current.withPoint(r, i)

        assert(current.mean === mean)
        assert(current.variance === variance)
        assert(current.pdf(1) === pdf)
      }
    }
  }
}
