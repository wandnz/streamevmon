package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.Logging

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.scalactic.{Equality, TolerantNumerics}

/** An implementation of a normal distribution whose parameters are based on
  * the points provided to it.
  *
  * @param n           The number of measurements that have been used to create the
  *                    distribution parameters. Note that using .withPoint allows the
  *                    caller to supply a custom n, so this might not actually reflect
  *                    the true number.
  * @param mapFunction The function to apply to elements of type T to obtain the
  *                    relevant data for this distribution.  For example, an
  *                    ICMP measurement would most likely have its latency value
  *                    extracted.
  *
  * @see [[https://en.wikipedia.org/wiki/Normal_distribution]]
  */
case class NormalDistribution[T: TypeInformation](
  mean: Double,
  mapFunction: MapFunction[T],
  variance: Double = NormalDistribution.defaultVariance,
  n: Int = 0
)
  extends Distribution[T] with Logging {

  import NormalDistribution._

  override def toString: String = {
    s"${getClass.getSimpleName}(n=$n,mean=$mean,variance=$variance)"
  }

  override val distributionName: String = "Normal Distribution"

  override def pdf(x: T): Double = {
    val y = mapFunction(x)

    // If the variance is 0, we should instead use some other small
    // value to prevent the PDF function from becoming a delta function,
    // which is 0 at all places except the mean, at which it is infinite.
    val maybeFakeVariance = if (doubleEquality.areEqual(variance, 0.0)) {
      logger.warn("PDF called with variance == 0!")
      y / 100
    }
    else {
      variance
    }

    import java.lang.Math._
    val a = 1.0 / (sqrt(2.0 * PI) * sqrt(maybeFakeVariance))
    val b = a * exp(-(((y - mean) * (y - mean)) / (2.0 * maybeFakeVariance)))
    b
  }

  override def withPoint(newT: T, newN: Int): NormalDistribution[T] = {
    val fakeNForMean = if (newN == 1) {
      0
    }
    else {
      newN
    }
    val newValue = mapFunction(newT)
    val newMean = ((mean * fakeNForMean) + newValue) / (fakeNForMean + 1)
    val diff = (newValue - newMean) * (newValue - mean)
    val newVariance = (variance * newN + diff) / (newN + 1)

    NormalDistribution(newMean, mapFunction, newVariance, newN)
  }
}

/** Companion object with some constructors and the default variance value.
  */
object NormalDistribution {

  @transient implicit private[changepoint] val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(1E-15)

  private[changepoint] val defaultVariance: Double = 1E8

  def apply[T: TypeInformation](dist: NormalDistribution[T]): NormalDistribution[T] = {
    NormalDistribution(dist.mean, dist.mapFunction, dist.variance, dist.n)
  }

  def apply[T: TypeInformation](item: T, mapFunction: MapFunction[T]): NormalDistribution[T] = {
    NormalDistribution(mapFunction(item), mapFunction, defaultVariance, n = 1)
  }
}
