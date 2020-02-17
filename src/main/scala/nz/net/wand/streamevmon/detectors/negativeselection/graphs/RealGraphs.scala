package nz.net.wand.streamevmon.detectors.negativeselection.graphs

import nz.net.wand.streamevmon.detectors.negativeselection.{Detector, DetectorGenerationMethod, DetectorGenerator}

import java.awt.{Color, Dimension, Rectangle}
import java.io.{File, FileOutputStream, OutputStreamWriter}

import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.jfree.chart.{ChartUtils, JFreeChart}
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.{DatasetRenderingOrder, XYPlot}
import org.jfree.chart.renderer.xy.{StandardXYItemRenderer, XYBubbleRenderer}
import org.jfree.chart.util.ShapeUtils
import org.jfree.data.xy.{DefaultXYDataset, DefaultXYZDataset}

class RealGraphs(
  filename: String = "./out/graphs/rnsap"
) extends RnsapGraphs {
  override def createGraph(
    detectors: Iterable[Detector],
    generator: DetectorGenerator,
    selfData: Iterable[Iterable[Double]],
    nonselfData: Iterable[Iterable[Double]],
    dimensionRanges: Iterable[(Double, Double)],
    dimensionNames: Iterable[String],
    generationMethod: DetectorGenerationMethod
  ): Unit = {

    val detectorsDataset = new DefaultXYZDataset()
    if (detectors.nonEmpty) {
      detectorsDataset.addSeries(
        "Detectors",
        Array(
          detectors.map(_.centre.head).toArray,
          detectors.map(_.centre.drop(1).head).toArray,
          detectors.map(d => math.sqrt(d.squareRadius) * 2).toArray
        )
      )
    }

    val selfDataset = new DefaultXYDataset()
    if (selfData.head.nonEmpty) {
      selfDataset.addSeries(
        "Self",
        Seq(selfData.map(_.head).toArray, selfData.map(_.drop(1).head).toArray).toArray
      )
    }

    val nonSelfDataset = new DefaultXYDataset()
    if (nonselfData.head.nonEmpty) {
      nonSelfDataset.addSeries(
        "Non-Self",
        Seq(nonselfData.map(_.head).toArray, nonselfData.map(_.drop(1).head).toArray).toArray
      )
    }

    val chart = new JFreeChart(
      "RNSAP",
      new XYPlot(
        null,
        new NumberAxis(dimensionNames.head),
        new NumberAxis(dimensionNames.drop(1).head),
        null
      )
    )

    val rangeXBuffer = (dimensionRanges.head._2 - dimensionRanges.head._1) * generationMethod.borderProportion
    val rangeYBuffer = (dimensionRanges.drop(1).head._2 - dimensionRanges.drop(1).head._1) * generationMethod.borderProportion
    chart.getXYPlot.getDomainAxis.setRange(
      dimensionRanges.head._1 - rangeXBuffer,
      dimensionRanges.head._2 + rangeXBuffer
    )
    chart.getXYPlot.getRangeAxis.setRange(
      dimensionRanges.drop(1).head._1 - rangeYBuffer,
      dimensionRanges.drop(1).head._2 + rangeYBuffer
    )

    // Detector bubbles
    chart.getXYPlot.setDataset(0, detectorsDataset)
    chart.getXYPlot.setRenderer(0, new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_BOTH_AXES))
    chart.getXYPlot.getRenderer(0).setSeriesPaint(0, new Color(0f, 0f, 1f, 0.05f))

    // Self data squares
    chart.getXYPlot.setDataset(1, selfDataset)
    chart.getXYPlot.setRenderer(1, new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
    chart.getXYPlot.getRenderer(1).setSeriesPaint(0, Color.GREEN)

    // Non-self data crosses
    chart.getXYPlot.setDataset(2, nonSelfDataset)
    chart.getXYPlot.setRenderer(2, new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
    chart.getXYPlot.getRenderer(2).setSeriesShape(0, ShapeUtils.createRegularCross(5, 1))
    chart.getXYPlot.getRenderer(2).setSeriesPaint(0, Color.RED)

    // Let's find the nearest self-point for each detector so we can draw lines.
    val detectorToNearestSelfDataset = new DefaultXYDataset()
    Range(0, detectors.size).zip(detectors).foreach { indexAndDetector =>
      val nearestSelf = generator.getClosestSelf(indexAndDetector._2.centre)

      detectorToNearestSelfDataset.addSeries(
        s"detector to nearest self ${indexAndDetector._1}",
        Array(
          Array(indexAndDetector._2.centre.head, nearestSelf._1.head),
          Array(indexAndDetector._2.centre.drop(1).head, nearestSelf._1.drop(1).head)
        )
      )
    }

    // We skip writing the legend for these lines.
    val legendItems = chart.getXYPlot.getLegendItems

    chart.getXYPlot.setDataset(3, detectorToNearestSelfDataset)
    chart.getXYPlot.setRenderer(3, new StandardXYItemRenderer(StandardXYItemRenderer.LINES))
    chart.getXYPlot.setFixedLegendItems(legendItems)

    chart.getXYPlot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD)

    ChartUtils.saveChartAsPNG(
      new File(s"$filename.png"),
      chart,
      1000,
      1000
    )

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
