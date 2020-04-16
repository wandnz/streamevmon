package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.AmpMeasurementSourceFunction
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.compat.Platform.EOL

object AmpMetricRunner {

  case class Metrics(elements: Iterable[Measurement]) {
    private lazy val elementsAsDoubles = elements.filter(_.defaultValue.isDefined).map(_.defaultValue.get)
    private lazy val elementsAsDoublesSorted = elementsAsDoubles.toSeq.sorted

    lazy val startTime: Instant = elements.minBy(_.time).time
    lazy val endTime: Instant = elements.maxBy(_.time).time

    lazy val mean: Double = elementsAsDoubles.sum / elementsAsDoubles.size
    lazy val median: Double = elementsAsDoublesSorted(elementsAsDoublesSorted.size / 2)

    def percentile(percent: Int): Double = {
      elementsAsDoublesSorted(math.ceil((elementsAsDoublesSorted.size - 1) * (percent / 100.0)).toInt)
    }

    override def toString: String = s"Metrics between $startTime and $endTime:" + EOL +
      s"Mean: $mean" + EOL +
      s"Median: $median" + EOL +
      s"75%: ${percentile(75)}" + EOL +
      s"Elements: $elementsAsDoublesSorted"
  }

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    System.setProperty("influx.dataSource.default.subscriptionName", "AmpMetricRunner")
    System.setProperty("flink.maxLateness", "0")
    env.getConfig.setGlobalJobParameters(Configuration.get(args))
    env.disableOperatorChaining()

    env.addSource(new AmpMeasurementSourceFunction)
      .keyBy(_ => 0)
      .windowAll(TumblingEventTimeWindows.of(Time.seconds(90)))
      .process {
        new ProcessAllWindowFunction[Measurement, Metrics, TimeWindow] {
          override def process(
            context : Context,
            elements: Iterable[Measurement],
            out     : Collector[Metrics]
          ): Unit = {
            out.collect(Metrics(elements))
          }
        }
      }
      .print("Metrics")

    env.execute()
  }
}
