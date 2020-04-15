package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.AmpMeasurementSourceFunction

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

object AmpMetricRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.default.subscriptionName", "AmpMetricRunner")
    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    val source = new AmpMeasurementSourceFunction

  }
}
