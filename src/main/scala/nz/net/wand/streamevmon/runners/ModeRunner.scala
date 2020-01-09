package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.flink.{LatencyTSAmpFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import java.io.File

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

/** Main runner for mode change detector, detailed in the
  * [[nz.net.wand.streamevmon.detectors.mode]] package.
  */
object ModeRunner {

  def doIt(args: Array[String], filename: String): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.default.subscriptionName", "ModeDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    //env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val source = env
      /*
          .addSource(new AmpMeasurementSourceFunction)
          .setParallelism(1)
          .name("Measurement Subscription")
          .uid("mode-measurement-sourcefunction")
      */
      .readFile(new LatencyTSAmpFileInputFormat, filename)
      .setParallelism(1)
      .name("Latency TS AMP Input")
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    val detector = new ModeDetector[LatencyTSAmpICMP]
    detector.enableGraphing("out/graphs/mode.svg", detector.detectorName)

    val process = source
      .process(detector)
      .name(detector.detectorName)
      .uid("mode-detector")

    /*
    process
      .addSink(new InfluxSinkFunction)
      .name("Influx Sink")
      .uid("mode-influx-sink")
     */

    process.print(s"Mode Event ($filename)")

    env.execute("Measurement subscription -> Mode Detector")
  }

  def main(args: Array[String]): Unit = {
    def getListOfFiles(dir: String): Seq[String] = {
      val file = new File(dir)
      file.listFiles
        .filter(_.isFile)
        .map(_.getPath)
        .toList
    }

    /*
    for (file <- getListOfFiles("data/latency-ts-i/ampicmp/series")) {
      println(file)

      doIt(args, file)
    }

     */
    doIt(args, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
  }
}
