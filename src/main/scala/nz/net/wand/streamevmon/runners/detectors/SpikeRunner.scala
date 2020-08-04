package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.spike.{SpikeDetail, SpikeDetector}
import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.time.Duration

import org.apache.flink.api.scala.operators.ScalaCsvOutputFormat
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

import scala.reflect.io.File

/** Main runner for spike detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.spike]] package.
  */
object SpikeRunner {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.setParallelism(1)

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP Input")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    val detector = new SpikeDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.flinkName)
      .uid(detector.flinkUid)

    process.print(s"Spike Signal")

    val csvOut = new ScalaCsvOutputFormat[(Double, Double, Double, Double, Double, Int)](new Path("./out/spike.csv"))

    process
      .getSideOutput(OutputTag[SpikeDetail]("detailed-output"))
      .map { x =>
        val tuple = SpikeDetail.unapply(x).get
        (tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6.id)
      }
      .writeUsingOutputFormat(csvOut)

    new File(new java.io.File("./out/spike.csv")).deleteRecursively()

    env.execute("Latency TS -> Spike Detector")
  }
}
