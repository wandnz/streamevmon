package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.events.{Event, ThresholdEvent}
import nz.net.wand.amp.analyser.flink.{InfluxSink, RichMeasurementSourceFunction}
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

    val eventStream = env
      .addSource(sourceFunction)
      .windowAll(TumblingEventTimeWindows.of(Time.seconds(10)))
      .process(new ProcessAllWindowFunction[RichMeasurement, Event, TimeWindow] {
        override def process(context: Context,
                             elements: Iterable[RichMeasurement],
                             out: Collector[Event]): Unit = {
          elements
            .filter(_.isInstanceOf[RichICMP])
            .map(_.asInstanceOf[RichICMP])
            .foreach(m => {
              if (m.stream > 2) {
                out.collect(ThresholdEvent(severity = 10, m.time))
              }
            })
        }
      })

    eventStream.print()
    eventStream.addSink(new InfluxSink())

    env.execute()

    sys.exit()
  }
}
