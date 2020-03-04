package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.graphs.{DummyGraphs, RnsapGraphs}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.{Measurement, SimpleIterableMeasurement}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}
import nz.net.wand.streamevmon.Logging

import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.util.Collector

class RnsapDetector[MeasT <: Measurement, W <: Window](
  detectorGenerationMethod: DetectorGenerationMethod,
  maxDimensions: Int,
  grapher                 : RnsapGraphs = new DummyGraphs
)
  extends ProcessWindowFunction[MeasT, Event, String, W]
          with Logging {

  def isAbnormalMeasurement(value: MeasT): Boolean = {
    value match {
      case h: Haberman => h.survivalStatus == SurvivalStatus.MoreThan5Years
      case i: SimpleIterableMeasurement => i.isAbnormal
      case _ => throw new NotImplementedError(
        "This measurement type cannot yet be classified as self or non-self."
      )
    }
  }

  private val hasTrainedState: ValueStateDescriptor[Boolean] =
    new ValueStateDescriptor[Boolean]("Has trained?", TypeInformation.of(classOf[Boolean]))

  private val trainTimeState: ValueStateDescriptor[Double] =
    new ValueStateDescriptor[Double]("Training time", TypeInformation.of(classOf[Double]))

  private val detectorsState: ValueStateDescriptor[Iterable[Detector]] =
    new ValueStateDescriptor[Iterable[Detector]](
      "Detectors",
      TypeInformation.of(classOf[Iterable[Detector]])
    )

  private val dimensionScaleFactorsState: ValueStateDescriptor[Iterable[(Double, Double)]] =
    new ValueStateDescriptor[Iterable[(Double, Double)]](
      "Dimension scale factors",
      TypeInformation.of(classOf[Iterable[(Double, Double)]])
    )

  def normaliseRawData(d: Iterable[Double], scaleFactors: Iterable[(Double, Double)]): Iterable[Double] = {
    d.take(maxDimensions).zip(scaleFactors).map(ds => (ds._1 - ds._2._1) / ds._2._2)
  }

  def normaliseRawDatas(d: Iterable[Iterable[Double]], scaleFactors: Iterable[(Double, Double)]): Iterable[Iterable[Double]] =
    d.map(e => normaliseRawData(e, scaleFactors))

  def measDataToRaw(d: Iterable[MeasT]): Iterable[Iterable[Double]] = {
    d.map(_.defaultValues.get.take(maxDimensions))
  }

  def normaliseMeasDataToRaw(d: Iterable[MeasT], scaleFactors: Iterable[(Double, Double)]): Iterable[Iterable[Double]] =
    normaliseRawDatas(measDataToRaw(d), scaleFactors)

  def train(
    elements: Iterable[MeasT],
    maxDimensions: Int
  ): Unit = {

    val startTime = System.nanoTime

    val elementsAsRaw = measDataToRaw(elements)

    // Determine the number of dimensions in the dataset
    val dimensions = elementsAsRaw.size

    // Find the minimum and maximum values for each dimension
    val mins = elementsAsRaw
      .reduce((a, b) => a.zip(b).map(c => math.min(c._1, c._2)))
    val maxs = elementsAsRaw
      .reduce((a, b) => a.zip(b).map(c => math.max(c._1, c._2)))

    // Pair the minimums and maximums to determine the range of each dimension.
    val dimensionRanges = mins.zip(maxs)

    val dimensionScaleFactors = dimensionRanges.map(r => (r._1, r._2 - r._1))

    getRuntimeContext.getState(dimensionScaleFactorsState).update(dimensionScaleFactors)

    // Scale every dimension to [0,1]
    //val elementsScaled = normaliseRawDatas(elementsAsRaw, dimensionScaleFactors)

    val (nonselfData, selfData) = elements.partition(isAbnormalMeasurement)
    val selfDataScaled = normaliseMeasDataToRaw(selfData, dimensionScaleFactors)
    val nonselfDataScaled = normaliseMeasDataToRaw(nonselfData, dimensionScaleFactors)

    logger.info(s"${selfData.size} normal elements, ${nonselfData.size} abnormal elements")

    val dimensionRangesScaled = dimensionRanges.map(_ => (0.0, 1.0))

    // Now that we know our number of dimensions, we can make a new
    // detector generator depending on the method we've been told to use.
    val detectorGenerator = DetectorGenerator(
      dimensions,
      selfDataScaled,
      nonselfDataScaled,
      dimensionRangesScaled,
      detectorGenerationMethod
    )

    // Let's generate a bunch of detectors and slap them into a graph as a test.
    val detectors = detectorGenerator.generateUntilDone()

    getRuntimeContext.getState(detectorsState).update(detectors)

    val endTime = System.nanoTime

    val trainTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime).toDouble
    getRuntimeContext.getState(trainTimeState).update(trainTime)

    logger.error(f"Training took ${trainTime / 1000}%1.3fs")

    grapher.createGraph(
      detectors = detectors,
      selfData = selfDataScaled,
      nonselfData = nonselfDataScaled,
      dimensionRanges = dimensionRangesScaled,
      generator = detectorGenerator,
      generationMethod = detectorGenerationMethod,
      filenameSuffix = "train"
    )
  }

  def test(
    elements: Iterable[MeasT],
    maxDimensions: Int
  ): Unit = {
    val startTime = System.nanoTime

    val detectors = getRuntimeContext.getState(detectorsState).value()

    val (nonselfData, selfData) = elements.partition(isAbnormalMeasurement)
    logger.info(s"${selfData.size} normal elements, ${nonselfData.size} abnormal elements")
    logger.error(s"${detectors.size} detectors in use")

    val dimensionScaleFactors = getRuntimeContext.getState(dimensionScaleFactorsState).value()

    val elementsScaled = normaliseMeasDataToRaw(elements, dimensionScaleFactors)

    val isAbnormal = elementsScaled.map { e =>
      detectors.exists { d =>
        d.contains(e)
      }
    }

    val endTime = System.nanoTime()

    val shouldBeAbnormal = elements.map(isAbnormalMeasurement)
    val correct = (isAbnormal, shouldBeAbnormal).zipped.map((a, b) => a == b)
    val correctProportion = correct.groupBy(a => a).mapValues(l => l.size).getOrElse(true, 0).toDouble / correct.size

    val (correctlyAssignedPoints, incorrectlyAssignedPoints) = {
      val (good, notGood) = (elementsScaled, correct).zipped.partition(_._2)
      (good.map(_._1), notGood.map(_._1))
    }

    val testTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime).toDouble

    logger.warn(s"Detector abnormality results: ${isAbnormal.groupBy(a => a).mapValues(l => l.size)}")
    logger.warn(s"Real abnormality results:     ${elements.map(isAbnormalMeasurement).groupBy(a => a).mapValues(l => l.size)}")
    logger.error(f"Correct proportion:           ${correctProportion * 100}%1.2f%%")
    logger.error(f"Testing took ${testTime / 1000}%1.3fs")

    grapher.createGraph(
      detectors = Seq(),
      selfData = correctlyAssignedPoints.toSeq,
      nonselfData = incorrectlyAssignedPoints.toSeq,
      generator = null,
      generationMethod = detectorGenerationMethod,
      filenameSuffix = "test"
    )

    grapher.writeCsv(
      detectorGenerationMethod,
      correctProportion,
      detectors.size,
      getRuntimeContext.getState(trainTimeState).value(),
      testTime
    )
  }

  override def process(
    key: String,
    context: Context,
    elements: Iterable[MeasT],
    out: Collector[Event]
  ): Unit = {

    logger.info(
      s"Executing ${context.window.getClass.getSimpleName} " +
        s"with ${elements.size} elements " +
        s"in ${elements.head.defaultValues.get.size} dimensions!"
    )

    val hasTrained = getRuntimeContext.getState(hasTrainedState)

    if (!hasTrained.value()) {
      train(elements, maxDimensions)
      hasTrained.update(true)
    }
    else {
      test(elements, maxDimensions)
    }
  }
}
