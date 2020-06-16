package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.flink.{MeasurementKeySelector, WindowedFunctionWrapper}
import nz.net.wand.streamevmon.measurements.InfluxMeasurement
import nz.net.wand.streamevmon.measurements.amp.Traceroute

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

object OutOfOrderRunner extends Logging {

  class MyReallyFunOutOfOrderSourceFunction extends SourceFunction[InfluxMeasurement] {

    def collect(ctx: SourceFunction.SourceContext[InfluxMeasurement], id: Int): Unit = {
      ctx.collectWithTimestamp(
        Traceroute(
          0,
          Some(1),
          Instant.ofEpochMilli(1000000000000L + TimeUnit.SECONDS.toMillis(id))
        ),
        1000000000000L + TimeUnit.SECONDS.toMillis(id)
      )
    }

    override def run(ctx: SourceFunction.SourceContext[InfluxMeasurement]): Unit = {
      for (x <- Range(0, 20)) {
        collect(ctx, x)
      }
      for (x <- Range(23, 25)) {
        collect(ctx, x)
      }
      for (x <- Range(20, 23)) {
        collect(ctx, x)
      }
      for (x <- Range(25, 50)) {
        collect(ctx, x)
      }
    }

    override def cancel(): Unit = {}
  }

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.default.subscriptionName", "OutOfOrderRunner")

    env.getConfig.setGlobalJobParameters(Configuration.get(Array()))
    env.setParallelism(1)

    val detector = new ModeDetector[InfluxMeasurement]
    val wrappedDetector = new WindowedFunctionWrapper[InfluxMeasurement, TimeWindow](
      detector
    )

    val s = env
      .addSource(new MyReallyFunOutOfOrderSourceFunction)
      .setParallelism(1)
      .name("Fun Source")

    val sk = s
      .keyBy(new MeasurementKeySelector[InfluxMeasurement])
      .timeWindow(Time.seconds(18))

    val p = sk
      .process(wrappedDetector)
      .name(detector.detectorName)
      .uid("mode-detector")

    val pr = p
      .print("Sink")

    env.execute("Measurement subscription -> Mode Detector")
  }
}
