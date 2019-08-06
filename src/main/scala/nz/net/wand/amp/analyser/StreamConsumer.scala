package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.flink.RichMeasurementSourceFunction
import nz.net.wand.amp.analyser.measurements.{RichICMP, RichMeasurement}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.util.Collector

object StreamConsumer {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val sourceFunction = new RichMeasurementSourceFunction

    env
      .addSource(sourceFunction)
      .windowAll(TumblingEventTimeWindows.of(Time.seconds(60)))
      .process(new ProcessAllWindowFunction[RichMeasurement, RichMeasurement, TimeWindow] {
        override def process(context: Context,
                             elements: Iterable[RichMeasurement],
                             out: Collector[RichMeasurement]): Unit = {
          elements
            .filter(m => m.isInstanceOf[RichICMP])
            .foreach(out.collect)
        }
      })
      .print()

    env.execute()

    sys.exit()
  }
}
