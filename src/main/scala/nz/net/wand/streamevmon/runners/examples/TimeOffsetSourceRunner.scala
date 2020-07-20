package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, PollingInfluxSourceFunction}
import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, InfluxMeasurementFactory}

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._

/** Shows the usage of the [[nz.net.wand.streamevmon.flink.PollingInfluxSourceFunction PollingInfluxSourceFunction]]
  * to gather "live" data with a time offset. Outputs measurements arriving in
  * real-time with an [[nz.net.wand.streamevmon.flink.AmpMeasurementSourceFunction AmpMeasurementSourceFunction]],
  * as well as measurements arriving a minute ago.
  *
  */
object TimeOffsetSourceRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // Sets the subscription name for the AmpMeasurementSourceFunction.
    System.setProperty("influx.dataSource.default.subscriptionName", "LiveSource")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))
    env.setParallelism(1)

    // Grab real-time data, and print its class and time.
    env
      .addSource(
        new AmpMeasurementSourceFunction(
          fetchHistory = Duration.ofMinutes(1)
        )
      )
      .name("Measurement Subscription")
      .map(i => s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement Now")

    // Grab data from a minute ago, and print its class and time.
    env
      .addSource {
        val s = new PollingInfluxSourceFunction[InfluxMeasurement](
          fetchHistory = Duration.ofMinutes(1),
          timeOffset = Duration.ofMinutes(1)
        ) {
          override protected[this] def processLine(line: String): Option[InfluxMeasurement] = InfluxMeasurementFactory.createMeasurement(line)
        }
        // TODO: Check that we actually need this separate configuration.
        //  Since this is a polling function, it shouldn't subscribe and won't
        //  overwrite the subscription for the real-time function.
        s.overrideConfig(
          env.getConfig.getGlobalJobParameters.asInstanceOf[ParameterTool].mergeWith(
            ParameterTool.fromMap(Map(
              "influx.dataSource.default.subscriptionName" -> "TimeOffsetSource"
            ).asJava)
          ))
        s
      }
      .name("Measurement Subscription")
      .map(i => s"${i.getClass.getSimpleName}(${new SimpleDateFormat("HH:mm:ss").format(Date.from(i.time))})")
      .print("Measurement Then")

    env.execute("Real-time and one-minute-ago printer")
  }
}