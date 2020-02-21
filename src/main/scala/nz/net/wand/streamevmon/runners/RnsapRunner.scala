package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.negativeselection.{DetectorGenerationMethod, RnsapDetector, TrainingDataSplitWindowAssigner}
import nz.net.wand.streamevmon.detectors.negativeselection.graphs.RealGraphs
import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod._
import nz.net.wand.streamevmon.flink.{HabermanFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.io.Source

object RnsapRunner {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    val filename = "data/haberman/haberman.data"

    val format = new HabermanFileInputFormat(0)

    val fileReader = Source.fromFile(filename)
    val (normal, anomalous) = fileReader.getLines().map { l =>
      val bytes = l.getBytes
      format.readRecord(null, bytes, 0, bytes.length)
    }.partition(_.survivalStatus == SurvivalStatus.LessThan5Years)
    val normalCount = normal.size
    val anomalousCount = anomalous.size
    fileReader.close()

    val input = env
      .readFile(format, filename)
      .assignAscendingTimestamps(_.time.toEpochMilli)

    input
      .keyBy(new MeasurementKeySelector[Haberman])
      .window(new TrainingDataSplitWindowAssigner[Haberman](
        normalCount,
        anomalousCount,
        randomSeed = Some(42L)
      ))
      .process(new RnsapDetector[Haberman, TimeWindow](
        DetectorGenerationMethod(
          detectorRadiusMethod = NearestSelfSampleRadius(),
          redundancy = true,
          spatialPreference = false,
          featurePreference = false,
          borderProportion = 0.0,
          detectorRedundancyProportion = 0.8,
          detectorRedundancyTerminationThreshold = 0.9
        ),
        new RealGraphs
      ))

    env.execute()
  }
}
