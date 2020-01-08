package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.MeasurementSourceFunction

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

object InfluxSourceReceiver {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)

    System.setProperty("influx.dataSource.default.subscriptionName", "InfluxSourceReceiver")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env
      .addSource(new MeasurementSourceFunction(fetchHistory = Duration.ofMinutes(1)))
      .name("Measurement Subscription")
      .map(i =>
        s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement")

    env.execute("InfluxDB subscription printer")
  }
}
