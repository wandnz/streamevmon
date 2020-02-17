package nz.net.wand.streamevmon.detectors.negativeselection.graphs

import nz.net.wand.streamevmon.detectors.negativeselection.{Detector, DetectorGenerator}

import java.awt.Color
import java.io.File

import org.jfree.chart.{ChartUtils, JFreeChart}
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.{StandardXYItemRenderer, XYBubbleRenderer}
import org.jfree.chart.util.ShapeUtils
import org.jfree.data.xy.{DefaultXYDataset, DefaultXYZDataset}

class RealGraphs(
  filename: String = "./out/graphs/rnsap.png"
) extends RnsapGraphs {
  override def createGraph(
    detectors: Iterable[Detector],
    selfData: Iterable[Iterable[Double]],
    nonselfData: Iterable[Iterable[Double]],
    dimensionRanges: Iterable[(Double, Double)],
    dimensionNames: Iterable[String]
  ): Unit = {

    val detectorsDataset = new DefaultXYZDataset()
    if (detectors.nonEmpty) {
      detectorsDataset.addSeries(
        "Detectors",
        Array(
          detectors.map(_.centre.head).toArray,
          detectors.map(_.centre.drop(1).head).toArray,
          detectors.map(_.radius * 2).toArray
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

    chart.getXYPlot.getDomainAxis.setRange(dimensionRanges.head._1, dimensionRanges.head._2)
    chart.getXYPlot.getRangeAxis.setRange(dimensionRanges.drop(1).head._1, dimensionRanges.drop(1).head._2)

    // Detector bubbles
    chart.getXYPlot.setDataset(0, detectorsDataset)
    chart.getXYPlot.setRenderer(0, new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_BOTH_AXES))
    chart.getXYPlot.getRenderer(0).setSeriesPaint(0, new Color(0f, 0f, 1f, 0.2f))

    // Self data squares
    chart.getXYPlot.setDataset(1, selfDataset)
    chart.getXYPlot.setRenderer(1, new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
    chart.getXYPlot.getRenderer(1).setSeriesPaint(0, Color.GREEN)

    // Non-self data crosses
    chart.getXYPlot.setDataset(2, nonSelfDataset)
    chart.getXYPlot.setRenderer(2, new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES))
    chart.getXYPlot.getRenderer(2).setSeriesShape(0, ShapeUtils.createRegularCross(5, 1))
    chart.getXYPlot.getRenderer(2).setSeriesPaint(0, Color.RED)

    // We'll make our own generator so we can use its getClosestSelf method
    // to draw lines from the centres of our detectors to their nearest self
    // measurements.
    val detectorGenerator = DetectorGenerator(
      dimensionRanges.size,
      selfData,
      nonselfData,
      dimensionRanges.toSeq
    )

    println()

    val detectorToNearestSelfDataset = new DefaultXYDataset()
    Range(0, detectors.size).zip(detectors).foreach { indexAndDetector =>
      val nearestSelf = detectorGenerator.getClosestSelf(indexAndDetector._2.centre)

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

    ChartUtils.saveChartAsPNG(
      new File(filename),
      chart,
      1000,
      1000
    )
  }
}
