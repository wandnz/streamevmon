package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.{Configuration, TestBase}
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.amp.TraceroutePathlen
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.util.Collector

class MyReallyFunOutOfOrderSourceFunction extends SourceFunction[InfluxMeasurement] with Serializable {
  def collect(ctx: SourceFunction.SourceContext[InfluxMeasurement], id: Int): Unit = {
    ctx.collectWithTimestamp(
      TraceroutePathlen(
        ":)",
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

class AwesomeCheckOnlyIncreasingTimeFunction extends KeyedProcessFunction[String, InfluxMeasurement, Event]
                                                     with Serializable
                                                     with HasFlinkConfig {
  var lastTimeObserved = Instant.MIN

  override def processElement(value: InfluxMeasurement, ctx: KeyedProcessFunction[String, InfluxMeasurement, Event]#Context, out: Collector[Event]): Unit = {
    assert(value.time.compareTo(lastTimeObserved) >= 0)
    lastTimeObserved = value.time
  }

  override val flinkName = ""
  override val flinkUid = ""
  override val configKeyGroup = ""
}

class WindowedFunctionWrapperTest extends TestBase {
  "WindowedFunctionWrapper" should {
    "order items correctly within a window" in {
      val env = StreamExecutionEnvironment.getExecutionEnvironment
      env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

      env.getConfig.setGlobalJobParameters(Configuration.get(Array()))
      env.setParallelism(1)

      env
        .addSource(new MyReallyFunOutOfOrderSourceFunction)
        .setParallelism(1)
        .name("Fun Source")
        .keyBy(new MeasurementKeySelector[InfluxMeasurement])
        .timeWindow(Time.seconds(18))
        .process(
          new WindowedFunctionWrapper[InfluxMeasurement, TimeWindow](
            new AwesomeCheckOnlyIncreasingTimeFunction
          )
        )
        .name("Print Input Time")

      env.execute("Measurement subscription -> Print time")
    }
  }
}
