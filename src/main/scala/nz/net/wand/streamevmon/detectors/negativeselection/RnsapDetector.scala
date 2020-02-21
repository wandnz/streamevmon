package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.graphs.{DummyGraphs, RnsapGraphs}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}
import nz.net.wand.streamevmon.Logging

import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.util.Collector

class RnsapDetector[MeasT <: Measurement, W <: Window](
  detectorGenerationMethod: DetectorGenerationMethod,
  grapher                 : RnsapGraphs = new DummyGraphs
)
  extends ProcessWindowFunction[MeasT, Event, String, W]
          with Logging {

  def isAbnormalMeasurement(value: MeasT): Boolean = {
    value match {
      case h: Haberman => h.survivalStatus == SurvivalStatus.LessThan5Years
      case _ => throw new NotImplementedError(
        "This measurement type cannot yet be classified as self or non-self."
      )
    }
  }

  override def process(
    key                   : String,
    context               : Context,
    elements              : Iterable[MeasT],
    out                   : Collector[Event]
  ): Unit = {

    logger.info(s"Executing ${context.window.getClass.getSimpleName} with ${elements.size} elements!")

    val maxDimensions = 2

    def normaliseRawData(d: Iterable[Double], scaleFactors: Iterable[(Double, Double)]): Iterable[Double] = {
      d.take(maxDimensions).zip(scaleFactors).map(ds => (ds._1 - ds._2._1) / ds._2._2)
    }

    def normaliseRawDatas(d: Iterable[Iterable[Double]], scaleFactors: Iterable[(Double, Double)]): Iterable[Iterable[Double]] =
      d.map(e => normaliseRawData(e, scaleFactors))

    def measDataToRaw(d: Iterable[MeasT]): Iterable[Iterable[Double]] = {
      d.map(_.defaultValues.get.take(maxDimensions))
    }

    def normaliseMeasDataToRaw(d                               : Iterable[MeasT], scaleFactors: Iterable[(Double, Double)]): Iterable[Iterable[Double]] =
      normaliseRawDatas(measDataToRaw(d), scaleFactors)

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

    // Scale every dimension to [0,1]
    val elementsScaled = normaliseRawDatas(elementsAsRaw, dimensionScaleFactors)

    val (selfData, nonselfData) = elements.partition(isAbnormalMeasurement)
    val selfDataScaled = normaliseMeasDataToRaw(selfData, dimensionScaleFactors)
    val nonselfDataScaled = normaliseMeasDataToRaw(nonselfData, dimensionScaleFactors)

    logger.debug(s"${selfData.size} normal elements, ${nonselfData.size} abnormal elements")

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

    grapher.createGraph(
      detectors = detectors,
      selfData = selfDataScaled,
      nonselfData = nonselfDataScaled,
      dimensionRanges = dimensionRangesScaled,
      generator = detectorGenerator,
      generationMethod = detectorGenerationMethod
    )
  }
}
