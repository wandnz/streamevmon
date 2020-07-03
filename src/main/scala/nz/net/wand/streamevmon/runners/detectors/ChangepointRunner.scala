package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, InfluxSinkFunction, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.InfluxMeasurement
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint._
import nz.net.wand.streamevmon.measurements.amp.ICMP

import java.time.Duration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

/** This is the main runner for the changepoint detector, which is
  * found in the [[nz.net.wand.streamevmon.detectors.changepoint]] package.
  *
  * @see [[nz.net.wand.streamevmon.detectors.changepoint the package description]] for details.
  * @see [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointGraphs ChangepointGraphs]] for an alternative bulk runner.
  */
object ChangepointRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.default.subscriptionName", "ChangepointDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      .addSource(new AmpMeasurementSourceFunction)
      .name("Measurement Subscription")
      .uid("changepoint-measurement-sourcefunction")
      .filter(_.isInstanceOf[ICMP])
      .name("Is ICMP?")
      .uid("changepoint-filter-is-icmp")
      .filter(!_.isLossy)
      .name("Has data?")
      .uid("changepoint-filter-has-data")
      .keyBy(new MeasurementKeySelector[InfluxMeasurement])

    implicit val ti: TypeInformation[NormalDistribution[InfluxMeasurement]] = TypeInformation.of(classOf[NormalDistribution[InfluxMeasurement]])

    val detector = new ChangepointDetector
                         [InfluxMeasurement, NormalDistribution[InfluxMeasurement]](
      new NormalDistribution(mean = 0)
    )

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .uid("changepoint-processor")

    process.addSink(new InfluxSinkFunction)
      .name("Influx Sink")
      .uid("changepoint-influx-sink")

    env.execute("Measurement subscription -> Changepoint Detector")
  }
}
