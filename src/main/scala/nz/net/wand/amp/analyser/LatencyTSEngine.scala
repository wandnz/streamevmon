package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.flink.LatencyTSSmokepingFileInputFormat

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

/** Entrypoint which parses the Latency TS I dataset.
  *
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
object LatencyTSEngine {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val format = new LatencyTSSmokepingFileInputFormat

    env
      .readFile(format, "data/latency-ts-i/smokeping/series")
      .print()

    env.execute()
  }
}
