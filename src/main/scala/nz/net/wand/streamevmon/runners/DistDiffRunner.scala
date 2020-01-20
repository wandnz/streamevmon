package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}

object DistDiffRunner {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.subscriptionName", "DistDiffDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val source = env
      .readFile(new LatencyTSAmpFileInputFormat, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      .name("Latency TS AMP ICMP")
      .uid("distdiff-source")
      .filter(_.lossrate == 0.0)
      .name("Has data?")
      .keyBy(_.stream)

    val detector = new DistDiffDetector[LatencyTSAmpICMP]

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .uid("distdiff-detector")
      .setParallelism(1)

    process.print("distdiff-printer")

    env.execute("Latency TS AMP ICMP -> Dist Diff Detector")
  }
}

object DistDiffTest {

  def main(args: Array[String]): Unit = {
    import org.apache.commons.math3.stat.inference.ChiSquareTest

    val c = new ChiSquareTest()

    val e: Array[Double] = Array(10, 10, 15, 20, 30, 15)
    val o: Array[Long] = Array(30, 14, 34, 105, 57, 62)

    // Raw chi square statistic, needs additional comparisons to get something useful
    println(c.chiSquare(e, o))
    // The probability that we obtain observed values based on the expected distribution
    println(c.chiSquareTest(e, o))
    // True if we can reject the null hypothesis - if the observed values do not match the expected distribution
    println(c.chiSquareTest(e, o, 0.05))

    println(c.chiSquareDataSetsComparison(e.map(_.toLong), o))
    println(c.chiSquareTestDataSetsComparison(e.map(_.toLong), o))
    println(c.chiSquareTestDataSetsComparison(e.map(_.toLong), o, 0.05))
  }
}
