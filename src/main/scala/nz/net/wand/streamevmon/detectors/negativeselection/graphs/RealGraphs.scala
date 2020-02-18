package nz.net.wand.streamevmon.detectors.negativeselection.graphs

import nz.net.wand.streamevmon.detectors.negativeselection.{Detector, DetectorGenerationMethod, DetectorGenerator}

import java.awt._
import java.io.{File, FileOutputStream, OutputStreamWriter}

import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.jfree.chart.{ChartUtils, JFreeChart}
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.{DatasetRenderingOrder, XYPlot}
import org.jfree.chart.renderer.xy.{StandardXYItemRenderer, XYBubbleRenderer}
import org.jfree.chart.util.ShapeUtils
import org.jfree.data.xy.{DefaultXYDataset, DefaultXYZDataset}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

class RealGraphs(
  filename: String = "./out/graphs/rnsap"
) extends RnsapGraphs {

  // Iterables should just be arrays whenever they need to be.
  implicit def iterableToArray[T: ClassTag](i: Iterable[T]): Array[T] = i.toArray

  implicit def doubleIterableToArray[T: ClassTag](i: Iterable[Iterable[T]]): Array[Array[T]] =
    i.map(_.toArray).toArray

  implicit def arrayOfBuffersToArrays[T: ClassTag](i: Array[mutable.Buffer[T]]): Array[Array[T]] =
    i.map(_.toArray)

  private def getNextDatasetIndex(chart: JFreeChart): Int = {
    val count = chart.getXYPlot.getDatasetCount
    if (count == 1 && chart.getXYPlot.getDataset(0) == null) {
      0
    }
    else {
      count
    }
  }

  private def addCircles(
    chart: JFreeChart,
    name : String,
    color: Paint,
    data : Array[Array[Double]]
  ): Unit = {
    val idx = getNextDatasetIndex(chart)

    if (data.nonEmpty) {
      val dataset = new DefaultXYZDataset()
      dataset.addSeries(name, data)

      chart.getXYPlot.setDataset(idx, dataset)
      chart.getXYPlot.setRenderer(idx, new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_BOTH_AXES))
      chart.getXYPlot.getRenderer(idx).setSeriesPaint(0, color)
    }
  }

  private def addDetectors(
    chart: JFreeChart,
    name : String,
    color: Paint,
    detectors: Iterable[Detector]
  ): Unit = {
    val data = Array(
      detectors.map(_.centre.head).toArray,
      detectors.map(_.centre.drop(1).head).toArray,
      detectors.map(d => math.sqrt(d.squareRadius) * 2).toArray
    )

    addCircles(chart, name, color, data)
  }

  private def addDetectorToNearestSelfLines(
    chart: JFreeChart,
    detectors: Iterable[Detector],
    selfData : Iterable[Iterable[Double]]
  ): Unit = {
    val dataset = new DefaultXYDataset()

    detectors.foreach { detector =>
      dataset.addSeries(
        s"detector to nearest self ${detector.centre}",
        Array(
          Array(detector.centre.head, detector.nearestSelfpoint.head),
          Array(detector.centre.drop(1).head, detector.nearestSelfpoint.drop(1).head)
        )
      )
    }
    val idx = getNextDatasetIndex(chart)
    chart.getXYPlot.setDataset(idx, dataset)
    chart.getXYPlot.setRenderer(idx, new StandardXYItemRenderer(StandardXYItemRenderer.LINES))
    // We can't set the colours for these lines for some reason.
  }

  private def addDetectorExclusionZones(
    chart: JFreeChart,
    drawInnerCircle: Boolean,
    drawOuterCircle: Boolean,
    innerCircleColor: Paint,
    bigCircleColor: Paint,
    detectors: Iterable[Detector],
    selfData: Iterable[Iterable[Double]],
    method: DetectorGenerationMethod
  ): Unit = {

    val smallCircles: Array[mutable.Buffer[Double]] = Array(mutable.Buffer(), mutable.Buffer(), mutable.Buffer())
    val bigCircles: Array[mutable.Buffer[Double]] = Array(mutable.Buffer(), mutable.Buffer(), mutable.Buffer())

    detectors.foreach { detector =>
      smallCircles.head.append(detector.centre.head)
      smallCircles.drop(1).head.append(detector.centre.drop(1).head)
      smallCircles.drop(2).head.append(math.sqrt(detector.squareRadius) * 2 * method.detectorRedundancyProportion)

      bigCircles.head.append(detector.nearestSelfpoint.head)
      bigCircles.drop(1).head.append(detector.nearestSelfpoint.drop(1).head)
      bigCircles.drop(2).head.append(math.sqrt(detector.squareRadius) * 2)
    }

    val idx = getNextDatasetIndex(chart)
    val dataset = new DefaultXYZDataset()
    if (drawInnerCircle) {
      dataset.addSeries("Redundancy inner-zone circles", smallCircles)
    }
    if (drawOuterCircle) {
      dataset.addSeries("Redundancy outer-zone circles", bigCircles)
    }
    chart.getXYPlot.setDataset(idx, dataset)
    chart.getXYPlot.setRenderer(idx, new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_BOTH_AXES))
    chart.getXYPlot.getRenderer(idx).setSeriesPaint(0, innerCircleColor)
    chart.getXYPlot.getRenderer(idx).setSeriesPaint(1, bigCircleColor)
  }

  private def addPoints(
    chart: JFreeChart,
    name : String,
    color: Paint,
    shape: Option[Shape],
    data : Array[Array[Double]]
  ): Unit = {
    val idx = getNextDatasetIndex(chart)

    val dataset = new DefaultXYDataset()
    if (data.head.nonEmpty) {
      dataset.addSeries(
        name,
        Seq(data.map(_.head), data.map(_.drop(1).head)).toArray
      )
    }
    chart.getXYPlot.setDataset(idx, dataset)
    chart.getXYPlot.setRenderer(idx, new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
    shape.foreach(s => chart.getXYPlot.getRenderer(idx).setSeriesShape(0, s))
    chart.getXYPlot.getRenderer(idx).setSeriesPaint(0, color)
  }

  override def createGraph(
    detectors: Iterable[Detector],
    generator: DetectorGenerator,
    selfData: Iterable[Iterable[Double]],
    nonselfData: Iterable[Iterable[Double]],
    dimensionRanges: Iterable[(Double, Double)],
    dimensionNames: Iterable[String],
    generationMethod: DetectorGenerationMethod
  ): Unit = {

    // First, let's set up the chart along with its X and Y bounds including the buffer.
    val chart = new JFreeChart(
      "RNSAP",
      new XYPlot(
        null,
        new NumberAxis(dimensionNames.head),
        new NumberAxis(dimensionNames.drop(1).head),
        null
      )
    )

    val rangeXBuffer = (dimensionRanges.head._2 - dimensionRanges.head._1) *
      generationMethod.borderProportion
    val rangeYBuffer = (dimensionRanges.drop(1).head._2 - dimensionRanges.drop(1).head._1) *
      generationMethod.borderProportion

    chart.getXYPlot.getDomainAxis.setRange(
      dimensionRanges.head._1 - rangeXBuffer,
      dimensionRanges.head._2 + rangeXBuffer
    )
    chart.getXYPlot.getRangeAxis.setRange(
      dimensionRanges.drop(1).head._1 - rangeYBuffer,
      dimensionRanges.drop(1).head._2 + rangeYBuffer
    )

    // Now let's add the data, in top-to-bottom rendering order.

    // The self-data shows as green squares.
    addPoints(chart,
      "Self",
      Color.GREEN,
      None,
      selfData
    )

    // The non-self-data shows as red crosses.
    addPoints(
      chart,
      "Non-Self",
      Color.RED,
      Some(ShapeUtils.createRegularCross(4, 1)),
      nonselfData
    )

    // Circles representing detectors in transparent blue.
    addDetectors(
      chart,
      "Detectors",
      new Color(0f, 0f, 1f, 0.05f),
      detectors
    )

    // From here on, we write no legend entries.
    chart.getXYPlot.setFixedLegendItems(chart.getXYPlot.getLegendItems)

    // Add some centre-to-nearest-self lines.
    addDetectorToNearestSelfLines(
      chart,
      detectors,
      selfData
    )

    // Add some big circles that overlap with detectors to show their
    // exclusion zones.
    addDetectorExclusionZones(
      chart,
      drawInnerCircle = true,
      drawOuterCircle = true,
      new Color(1f, 0f, 0f, 0.05f),
      new Color(0f, 1f, 0f, 0.01f),
      detectors,
      selfData,
      generationMethod
    )

    // Finally, save it out in two formats.
    // PNG
    ChartUtils.saveChartAsPNG(
      new File(s"$filename.png"),
      chart,
      1000,
      1000
    )

    chart.getXYPlot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE)

    // SVG
    val document = GenericDOMImplementation.getDOMImplementation.createDocument(null, "svg", null)
    val svg = new SVGGraphics2D(document)
    svg.setSVGCanvasSize(new Dimension(1000, 1000))
    chart.draw(svg, new Rectangle(1000, 1000))
    val outputStream = new OutputStreamWriter(new FileOutputStream(new File(s"$filename.svg")))
    svg.stream(outputStream, true)
    outputStream.flush()
    outputStream.close()
  }
}
