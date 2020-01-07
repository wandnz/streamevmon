package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.{InfluxSinkFunction, MeasurementKeySelector, MeasurementSourceFunction}
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint._
import nz.net.wand.streamevmon.detectors.MapFunction
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

    System.setProperty("influx.dataSource.subscriptionName", "ChangepointDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      .addSource(new MeasurementSourceFunction)
      .name("Measurement Subscription")
      .uid("changepoint-measurement-sourcefunction")
      .filter(_.isInstanceOf[ICMP])
      .name("Is ICMP?")
      .uid("changepoint-filter-is-icmp")
      .filter(!_.isLossy)
      .name("Has data?")
      .uid("changepoint-filter-has-data")
      .keyBy(new MeasurementKeySelector[Measurement])

    implicit val ti: TypeInformation[NormalDistribution[Measurement]] = TypeInformation.of(classOf[NormalDistribution[Measurement]])

    class IcmpToMedian() extends MapFunction[Measurement, Double] with Serializable {
      override def apply(t: Measurement): Double = t.asInstanceOf[ICMP].median.get
    }

    val detector = new ChangepointDetector
                         [Measurement, NormalDistribution[Measurement]](
      new NormalDistribution(mean = 0, mapFunction = new IcmpToMedian)
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
