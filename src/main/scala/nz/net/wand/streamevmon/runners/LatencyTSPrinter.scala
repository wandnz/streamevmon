package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.Configuration

import org.apache.flink.api.scala.operators.ScalaCsvOutputFormat
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

/** Entrypoint which parses the Latency TS I dataset.
  *
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
object LatencyTSPrinter {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    val input = new LatencyTSAmpFileInputFormat
    val output: ScalaCsvOutputFormat[(Int, String, String, String, Long, Int, Double)] =
      new ScalaCsvOutputFormat(new Path("out/output.csv"))
    output.setWriteMode(WriteMode.OVERWRITE)

    env
      .readFile(input, "data/latency-ts-i/ampicmp/series/waikato-xero-ipv4.series")
      .setParallelism(1)
      //.print()
      .map(LatencyTSAmpICMP.unapply(_).get)
      .map(a => a.copy(_5 = a._5.toEpochMilli))
      .writeUsingOutputFormat(output)
      .setParallelism(1)

    env.execute()
  }
}
