package nz.net.wand.streamevmon.detectors.changepoint

import nz.net.wand.streamevmon.measurements.Measurement

import scala.collection.mutable

class RunHolder[MeasT <: Measurement, DistT <: Distribution[MeasT]](
    initialDistribution: DistT
) {

  var runs: mutable.Seq[(DistT, Double)] = mutable.Seq[(DistT, Double)]()

  def length: Int = runs.length
  def isEmpty: Boolean = runs.isEmpty

  def add(item: MeasT): Unit = {
    runs = runs :+ Tuple2(initialDistribution.withPoint(item), 1.0).asInstanceOf[(DistT, Double)]
  }

  def purge(): Unit = runs = mutable.Seq[(DistT, Double)]()

  /** @return The distribution with the highest probability. */
  def getMostProbable: Int = {
    if (runs.filter(_._2 >= 0.0).isEmpty) {
      -1
    }
    else {
      runs.zipWithIndex.maxBy(_._1._2)._2
    }
  }

  def normalise(): Unit = {
    val total = runs
      .map(_._2)
      .fold(0.0)((a, b) => a + b)

    // There's a special case here if the total is 0, since it means the PDF for
    // all our runs is 0. This'll happen if there's an outlier that doesn't fit
    // in any existing runs.

    runs.zipWithIndex
      .foreach { x =>
        runs.update(x._2, (x._1._1, x._1._2 / total))
      }
  }

  /** Reflects cpp::249-272
    *
    * @param hazard A smaller hazard tends to be less sensitive (larger lambda)
    *
    * @return The probability that there was a change point.
    */
  def applyGrowthProbabilities(item: MeasT, hazard: Double = 1.0 / 200.0): Unit = {
    var weight = 0.0

    // Update the probabilities of each run such that
    // runs[n+1].prob = runs[n].pdf * runs[n].prob * (1 - hazard)
    runs = runs
      .map { x =>
        (
          x._1.withPoint(item).asInstanceOf[DistT],
          if (x._1.n >= 2) {
            x._2 * x._1.pdf(item) * (1 - hazard)
          }
          else {
            x._2
          }
        )
      }

    weight = runs.filter(_._1.n > 2).map(_._2).sum
    if (weight == 0.0) {
      weight = 1.0
    }

    if (runs.filter(_._2.isNaN).nonEmpty) {
      println("AAAHHHH")
    }

    runs = Tuple2(initialDistribution.withPoint(item).asInstanceOf[DistT], weight) +: runs
  }

  def pruneRuns(maxHistory: Int): Unit = {
    while (runs.length > maxHistory) {
      runs(runs.length - 2) = (runs.last._1, runs.last._2 + runs(runs.length - 2)._2)
      runs = runs.dropRight(1)
    }
  }

  def setProbabilityToMax(which: Int): Unit = {
    runs = runs.zipWithIndex.map { x =>
      if (x._2 == which) {
        (x._1._1, 1.0)
      }
      else {
        (x._1._1, 0.0)
      }
    }
  }

  def trim(lastToKeep: Int): Unit = {
    runs = runs.dropRight(runs.length - (lastToKeep + 1))
  }
}
