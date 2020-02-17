package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.negativeselection.{DetectorGenerationMethod, RnsapDetector}
import nz.net.wand.streamevmon.detectors.negativeselection.graphs.RealGraphs
import nz.net.wand.streamevmon.flink.{HabermanFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.haberman.Haberman

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

object RnsapRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val format = new HabermanFileInputFormat(0)

    val input = env
      .readFile(format, "data/haberman/haberman.data")
      .assignAscendingTimestamps(_.time.toEpochMilli)

    input
      .keyBy(new MeasurementKeySelector[Haberman])
      .timeWindow(Time.days(1))
      .process(new RnsapDetector[Haberman, TimeWindow](
        DetectorGenerationMethod(
          redundancy = false,
          spatialPreference = false,
          featurePreference = false
        ),
        new RealGraphs
      ))

    env.execute()
  }
}
