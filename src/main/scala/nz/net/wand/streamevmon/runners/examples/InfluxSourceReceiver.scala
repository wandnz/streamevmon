package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

/** Most basic example of using an [[nz.net.wand.streamevmon.flink.sources.InfluxSourceFunction InfluxSourceFunction]],
  * in this case an [[nz.net.wand.streamevmon.flink.sources.AmpMeasurementSourceFunction AmpMeasurementSourceFunction]].
  *
  * Requires `source.influx.(amp.)?serverName` to be set.
  *
  * This just prints the type of measurement that was received and its time.
  */
object InfluxSourceReceiver {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)

    System.setProperty("source.influx.subscriptionName", "InfluxSourceReceiver")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env
      .addSource(new AmpMeasurementSourceFunction(fetchHistory = Duration.ofMinutes(1)))
      .name("Measurement Subscription")
      .map(i =>
        s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement")

    env.execute("InfluxDB subscription printer")
  }
}
