package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.TrainingDataSplitWindowAssigner.{TestingWindow, TrainingWindow}
import nz.net.wand.streamevmon.measurements.{Measurement, SimpleIterableMeasurement}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}

import java.util.{Collection => JCollection}

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner
import org.apache.flink.streaming.api.windowing.triggers.Trigger
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

/** This WindowAssigner will split the given input into training and testing
  * datasets, then emit them in that order.
  *
  * The elementCount variables must be set correctly, or the windows will not
  * be emitted correctly.
  *
  * Input data is expected to be bounded, but only the first
  * `(normalElementCount + anomalousElementCount)` elements will be added to
  * windows.
  *
  * The proportion variables affect how much of each type of data will be
  * included in each dataset. The two variables of the same type (normal and
  * anomalous) should add to no more than 1.0: setting `trainingSetNormalProportion`
  * and `testingSetNormalProportion` to 0.75 each will cause unexpected behaviour.
  *
  * @param randomSeed To get the same output across multiple runs, set this value.
  */
class TrainingDataSplitWindowAssigner[MeasT <: Measurement](
  normalElementCount: Int,
  anomalousElementCount         : Int,
  randomSeed                    : Option[Long],
  trainingSetNormalProportion   : Double = 1.0,
  trainingSetAnomalousProportion: Double = 0.0,
  testingSetNormalProportion    : Double = 0.0,
  testingSetAnomalousProportion : Double = 0.5
)
  extends WindowAssigner[MeasT, TimeWindow] {

  // The start time of the training window is before that of the testing window,
  // so that they arrive in the correct order. Their duration is as long as
  // possible, since this is designed to work on bounded datasets that might
  // be timestamped at any point in history.
  private def trainingWindow: TimeWindow = new TrainingWindow(0, Long.MaxValue - 1)

  private def testingWindow: TimeWindow = new TestingWindow(1, Long.MaxValue)

  // Figure out how many of each type of element each dataset should have
  // This might not round properly. Needs more testing if it becomes an issue.
  private val trainingSetNormalCount = (normalElementCount * trainingSetNormalProportion).round.toInt
  private val trainingSetAnomalousCount = (anomalousElementCount * trainingSetAnomalousProportion).round.toInt
  private val testingSetNormalCount = (normalElementCount * testingSetNormalProportion).round.toInt
  private val testingSetAnomalousCount = (anomalousElementCount * testingSetAnomalousProportion).round.toInt

  // If the seed is None, just generate randomly as usual.
  randomSeed.foreach(seed => Random.setSeed(seed))

  // Pick a random order for all the elements.
  private val normalElementRandomOrder = Random.shuffle(Range(0, normalElementCount).toList)
  private val anomalousElementRandomOrder = Random.shuffle(Range(0, anomalousElementCount).toList)

  // Then take the element indices we want according to the proportions requested.
  private val trainingSetNormalIndices = normalElementRandomOrder.take(trainingSetNormalCount)
  private val trainingSetAnomalousIndices = anomalousElementRandomOrder.take(trainingSetAnomalousCount)

  // The testing set takes elements from the back so that you can do a random
  // exclusive split between training and testing sets.
  private val testingSetNormalIndices = normalElementRandomOrder.reverse.take(testingSetNormalCount)
  private val testingSetAnomalousIndices = anomalousElementRandomOrder.reverse.take(testingSetAnomalousCount)

  // We need to keep track of how many of each type of measurement we've seen
  // so that we can window new ones correctly. These should probably be
  // ValueStates, but since the job this is designed for has a parallelism of 1
  // we can change that later :)
  private var normalElementCounter = 0
  private var anomalousElementCounter = 0

  private def isAbnormalMeasurement(value: MeasT): Boolean = {
    value match {
      case h: Haberman => h.survivalStatus != SurvivalStatus.LessThan5Years
      case i: SimpleIterableMeasurement => i.isAbnormal
      case _ => throw new NotImplementedError(
        "This measurement type cannot yet be classified as self or non-self."
      )
    }
  }

  override def assignWindows(
    element: MeasT,
    timestamp: Long,
    context: WindowAssigner.WindowAssignerContext
  ): JCollection[TimeWindow] = {

    // Figure out if it's abnormal data or not and make an extensible list to
    // hold the windows it goes in.
    val isAbnormal = isAbnormalMeasurement(element)
    val windows: mutable.Buffer[TimeWindow] = mutable.Buffer()

    // Training and testing sets might each contain both normal and anomalous
    // data. One measurement could be in both datasets if there was any overlap
    // in the proportions selected.
    if (isAbnormal) {
      if (trainingSetAnomalousIndices.contains(anomalousElementCounter)) {
        windows.append(trainingWindow)
      }
      if (testingSetAnomalousIndices.contains(anomalousElementCounter)) {
        windows.append(testingWindow)
      }
      anomalousElementCounter += 1
    }
    else {
      if (trainingSetNormalIndices.contains(normalElementCounter)) {
        windows.append(trainingWindow)
      }
      if (testingSetNormalIndices.contains(normalElementCounter)) {
        windows.append(testingWindow)
      }
      normalElementCounter += 1
    }

    windows.asJavaCollection
  }

  // Let's tell the user what proportions were selected in an easy-to-read way.
  override def toString: String =
    s"Training: " +
      s"${(trainingSetNormalProportion * 100).toInt}% normal ($trainingSetNormalCount/$normalElementCount), " +
      s"${(trainingSetAnomalousProportion * 100).toInt}% anomalous ($trainingSetAnomalousCount/$anomalousElementCount). " +
      s"Testing: " +
      s"${(testingSetNormalProportion * 100).toInt}% normal ($testingSetNormalCount/$normalElementCount), " +
      s"${(testingSetAnomalousProportion * 100).toInt}% anomalous ($testingSetAnomalousCount/$anomalousElementCount). "

  // We use a custom trigger along with this class, so it shouldn't be overriden
  // unless you want some wacky behaviour.
  override def getDefaultTrigger(env: StreamExecutionEnvironment): Trigger[MeasT, TimeWindow] = {
    new DualWindowCountTrigger[MeasT, TimeWindow, TrainingWindow, TestingWindow](
      trainingSetNormalCount + trainingSetAnomalousCount,
      testingSetNormalCount + testingSetAnomalousCount
    ).asInstanceOf[Trigger[MeasT, TimeWindow]]
  }

  override def getWindowSerializer(executionConfig: ExecutionConfig): TypeSerializer[TimeWindow] = new TimeWindow.Serializer()

  override def isEventTime: Boolean = true
}

object TrainingDataSplitWindowAssigner {

  /** A sub-type used to distinguish which window is emitted.
    *
    * @see [[TestingWindow]]
    */
  class TrainingWindow(start: Long, end: Long) extends TimeWindow(start, end)

  /** A sub-type used to distinguish which window is emitted.
    *
    * @see [[TrainingWindow]]
    */
  class TestingWindow(start: Long, end: Long) extends TimeWindow(start, end)

}
