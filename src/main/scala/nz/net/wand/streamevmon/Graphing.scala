package nz.net.wand.streamevmon

import java.awt._
import java.io.File
import java.time.Instant
import java.util.Date

import org.jfree.chart.{ChartFactory, LegendItem, LegendItemCollection}
import org.jfree.chart.plot.ValueMarker
import org.jfree.data.time.{Second, TimeSeries, TimeSeriesCollection}
import org.jfree.graphics2d.svg.{SVGGraphics2D, SVGUtils}

/** Allows an easier interface for producing graphs from detectors. Runners
  * should call enableGraphing on the instance while defining the Flink pipeline.
  * Implementations should call registerSeries the appropriate number of times
  * during setup, addToSeries for each point to be added to the graph, and
  * saveGraph during cleanup.
  */
trait Graphing {
  protected var doGraphs: Boolean = false
  protected var graphFilename: String = ""
  protected var graphTitle: String = "A Graph"

  protected val graphWidth: Int = 1600
  protected val graphHeight: Int = 900

  private var points: Map[String, Series] = _

  /** Turns on plotting for the object inheriting this trait.
    *
    * @param filename The destination of the resulting graph. Saved as an svg.
    * @param title    The title of the graph.
    */
  def enableGraphing(filename: String, title: String): Unit = {
    doGraphs = true
    graphFilename = filename
    graphTitle = title
    points = Map()
  }

  private case class SeriesConfig(
      paint: Paint,
      stroke: Stroke,
      kind: SeriesType.Value
  )

  private case class Point(
      value: Double,
      time: Long
  )

  private case class Series(
      points: Seq[Point],
      config: SeriesConfig
  )

  /** Registers a new series to go on the graph. For example, you might want
    * one series for the input values that might have a unique colour and
    * thickness, which is of type Line. You might have another series for the
    * events emitted, which is of type Events as the series is not continuous.
    *
    * @param name The name to use for the series. This will go on the graph
    *             legend, and is used to identify which series added points
    *             belong to.
    * @param kind Line or Events. Line will produce a continuous line, while
    *             Events produces vertical lines at the times given.
    * @param paint The colour to paint the line in. jawa.awt.Color is a good
    *              place to start.
    * @param thickness The thickness of the line. Default 1.
    */
  protected def registerSeries(
      name: String,
      kind: SeriesType.Value = SeriesType.Line,
      paint: Paint = Color.BLACK,
      thickness: Float = 1
  ): Unit = {
    if (doGraphs) {
      points = points + (name -> Series(Seq(), SeriesConfig(paint, new BasicStroke(thickness), kind)))
    }
  }

  /** Adds a point to a series. If the series has not yet been registered, it
    * will automatically register with default parameters. (Line type, black,
    * with default thickness).
    *
    * @param name The name of the series.
    * @param value The value of the point. Currently ignored for Events type.
    * @param time The time the value occurred at, in milliseconds since the
    *             Epoch.
    */
  protected def addToSeries(name: String, value: Double, time: Long): Unit = {
    if (doGraphs) {
      if (points.contains(name)) {
        points = points + (
          name -> Series(
            points(name).points :+ Point(value, time),
            points(name).config
          )
          )
      }
      else {
        registerSeries(name)
        addToSeries(name, value, time)
      }
    }
  }

  /** Adds a point to a series. If the series has not yet been registered, it
    * will automatically register with default parameters. (Line type, black,
    * with default thickness).
    *
    * @param name  The name of the series.
    * @param value The value of the point. Currently ignored for Events type.
    * @param time  The time the value occurred at.
    */
  protected def addToSeries(name: String, value: Double, time: Instant): Unit = {
    addToSeries(name, value, time.toEpochMilli)
  }

  /** Create and save the graph produced from all data given so far.
    */
  protected def saveGraph(): Unit = {
    if (!doGraphs) {
      return
    }

    // Set up some holders for the chart and data.
    val dataset = new TimeSeriesCollection()
    val chart = ChartFactory.createTimeSeriesChart(
      graphTitle,
      "Time",
      "Value",
      dataset
    )
    chart.getXYPlot.setBackgroundPaint(Color.BLACK)
    chart.getXYPlot.setDomainGridlinesVisible(false)
    chart.getXYPlot.setRangeGridlinesVisible(false)

    // First, we fill the dataset with the line type serieses.
    // This lets the chart automatically generate a legend for them.
    points
      .filter(_._2.config.kind == SeriesType.Line)
      .toList
      .sortBy(_._1)
      .zipWithIndex
      .foreach {
        case (p, i) =>
          val series = new TimeSeries(p._1)
          p._2.points.foreach { v =>
            series.add(new Second(new Date(v.time)), v.value)
          }
          dataset.addSeries(series)
          chart.getXYPlot.getRenderer.setSeriesPaint(i, p._2.config.paint)
          chart.getXYPlot.getRenderer.setSeriesStroke(i, p._2.config.stroke)
      }

    // Next, we fill in the event type serieses. These appear as vertical lines
    // at whatever time, but don't appear in the legend by default.
    points.filter(_._2.config.kind == SeriesType.Events).foreach { p =>
      // We'll add in the ValueMarkers so that the points show up.
      // Currently, the actual values get ignored since there's no good way to
      // represent that.
      p._2.points.foreach { v =>
        chart.getXYPlot.addDomainMarker(
          new ValueMarker(
            v.time,
            p._2.config.paint,
            p._2.config.stroke
          )
        )
      }

      // We'll also steal the automatically generated legend and add our own
      // entries to it. This is why we go over the line-type serieses first, so
      // that all their legend items are generated before we grab the collection
      // and put our own items in.
      chart.getXYPlot.setFixedLegendItems {
        var legendItems = Option(chart.getXYPlot.getFixedLegendItems)
        if (legendItems.isEmpty) {
          legendItems = Some(new LegendItemCollection())
          legendItems.get.addAll(chart.getXYPlot.getLegendItems)
        }
        legendItems.get.add(
          new LegendItem(
            p._1,
            p._2.config.paint
          )
        )
        legendItems.get
      }
    }

    // Next, we just slam the chart into an SVG file.
    val svg = new SVGGraphics2D(graphWidth, graphHeight)
    chart.draw(svg, new Rectangle(graphWidth, graphHeight))
    SVGUtils.writeToSVG(new File(graphFilename), svg.getSVGElement)
  }
}

protected object SeriesType extends Enumeration {
  @transient val Line, Events: Value = Value
}
