package nz.net.wand.streamevmon.detectors.negativeselection

import nz.net.wand.streamevmon.detectors.negativeselection.graphs.{DummyGraphs, RnsapGraphs}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.util.Collector

class RnsapDetector[MeasT <: Measurement, W <: Window](
  detectorGenerationMethod: DetectorGenerationMethod,
  grapher: RnsapGraphs = new DummyGraphs
)
  extends ProcessWindowFunction[MeasT, Event, String, W] {

  override def process(
    key                   : String,
    context               : Context,
    elements              : Iterable[MeasT],
    out                   : Collector[Event]
  ): Unit = {

    val elementsAsRaw = elements.map(_.defaultValues.get)

    // First, determine the number of dimensions of the dataset.
    val dimensions = elementsAsRaw.head.size

    // Next, find the minimum and maximum value for each dimension.
    val mins = elementsAsRaw
      .reduce((a, b) => a.zip(b).map(c => math.min(c._1, c._2)))
    val maxs = elementsAsRaw
      .reduce((a, b) => a.zip(b).map(c => math.max(c._1, c._2)))

    // Finally, pair the minimums and maximums to determine the range of each
    // dimension.
    val dimensionRanges = mins.zip(maxs)

    // Now that we know our number of dimensions, we can make a new
    // detector generator depending on the method we've been told to use.
    val detectorGenerator = DetectorGenerator(
      dimensions,
      dimensionRanges,
      detectorGenerationMethod
    )

    // Let's generate a bunch of detectors and slap them into a graph as a test.
    val detectors = detectorGenerator.generateN(10)

    grapher.createGraph(
      detectors = detectors,
      selfData = elementsAsRaw,
      nonselfData = Seq(Seq(), Seq()),
      dimensionRanges = dimensionRanges
    )
  }
}
