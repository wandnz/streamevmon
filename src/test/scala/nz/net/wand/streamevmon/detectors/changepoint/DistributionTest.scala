package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.measurements.{HasDefault, Measurement}

import java.time.Instant

import org.apache.flink.streaming.api.scala._

import scala.language.implicitConversions

class DistributionTest extends TestBase {

  import nz.net.wand.streamevmon.detectors.changepoint.NormalDistribution._

  "NormalDistribution" should {
    "generate the correct values" in {
      case class JustADouble(d: Double) extends Measurement with HasDefault {
        override val stream: Int = -1
        override val time: Instant = Instant.EPOCH

        override def isLossy: Boolean = false

        override var defaultValue: Option[Double] = Some(d)
      }

      val initial = new NormalDistribution[JustADouble](
        mean = 0.0,
        n = 0,
        variance = 1E8
      )

      // shouldEqual correctly uses scalactic DoubleEquality, while shouldBe doesn't.
      initial.mean shouldEqual 0.0
      initial.variance shouldEqual 1E8

      // I ran some maths externally to get the mean and PDF numbers. The variances
      // are just taken from the actual results, which isn't super useful, but
      // ensuring that they're pinned means the PDF will match up against what
      // I calculated.
      // They look pretty magical, but they do represent the expected parameters
      // for the given progression of inputs.
      // The normal distribution is a bit wacky in that it fakes how many elements
      // are used in the calculation of the first mean.
      initial.pdf(JustADouble(0)) shouldEqual 0.00003989422804014325
      initial.pdf(JustADouble(1)) shouldEqual 0.0000398942278406721

      var current = initial
      val numbersToAdd = Seq(1.0, 0.5, -1.0, 2.0, -3.0, 4.0, -5.0)
      val expectedMean = Seq(1.0, 2.5 / 3, 0.375, 0.7, 1.0 / 12, 9.0 / 14, -1.0 / 16)
      val expectedVar = Seq(5E7, 3.333333338888889E7, 2.5000000671875E7,
        2.000000096E7, 1.6666669368055558E7, 1.4285718479591837E7, 1.2500007152343752E7)
      val expectedPdf = Seq(0.00005641895835477566, 0.00006909882980789, 0.00007978845438478,
        0.00008920620346597, 0.00009772049199778, 0.00010555019017665, 0.00011283787933200)

      var i = 0
      for ((newPoint, mean, variance, pdf) <- (numbersToAdd zip expectedMean zip expectedVar zip expectedPdf)
             .map {
               case (((x, y), z), w) => (x, y, z, w)
             }) {
        i += 1
        current = current.withPoint(JustADouble(newPoint), i)

        current.mean shouldEqual mean
        current.variance shouldEqual variance
        current.pdf(JustADouble(1)) shouldEqual pdf
      }
    }
  }
}
