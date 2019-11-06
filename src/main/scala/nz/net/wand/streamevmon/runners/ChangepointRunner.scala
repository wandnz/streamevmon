package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.{InfluxSinkFunction, MeasurementSubscriptionSourceFunction}
import nz.net.wand.streamevmon.measurements.{ICMP, Measurement}
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint._
import nz.net.wand.streamevmon.events.ChangepointEvent

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

/** This is the main runner for the changepoint detector, which is
  * found in the [[nz.net.wand.streamevmon.detectors.changepoint]] package.
  *
  * This runner uses the [[nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]],
  * which must be supplied with files from the Latency TS I dataset.
  *
  * @see [[nz.net.wand.streamevmon.detectors.changepoint the package description]] for details.
  * @see [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointGraphs ChangepointGraphs]] for an alternative bulk runner.
  */
object ChangepointRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.subscriptionName", "ChangepointDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(5).toMillis, CheckpointingMode.EXACTLY_ONCE)

    env.registerType(classOf[NormalDistribution[Measurement]])

    val source = env
      .addSource(new MeasurementSubscriptionSourceFunction)
      .name("Measurement Subscription")
      .setParallelism(1)
      .filter(_.isInstanceOf[ICMP])
      .name("Is ICMP?")
      .filter(_.asInstanceOf[ICMP].loss == 0)
      .name("Has data?")
      .keyBy(_.stream)

    val detector = new ChangepointDetector
                         [Measurement, NormalDistribution[Measurement]](
      new NormalDistribution(mean = 0, mapFunction = _.asInstanceOf[ICMP].median.get)
    )

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .setParallelism(1)

    process.addSink(new InfluxSinkFunction[ChangepointEvent])
      .name("Influx Sink")

    env.execute("Measurement subscription -> Changepoint Detector")
  }
}
