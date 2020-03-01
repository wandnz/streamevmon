package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.detectors.negativeselection.{DetectorGenerationMethod, RnsapDetector, TrainingDataSplitWindowAssigner}
import nz.net.wand.streamevmon.detectors.negativeselection.graphs.RealGraphs
import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod._
import nz.net.wand.streamevmon.flink.{HabermanFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.io.Source

object RnsapRunner extends Logging {

  def doTheThing(method: DetectorGenerationMethod): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(Array())
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
        method,
        maxDimensions = Int.MaxValue,
        new RealGraphs
      ))

    env.execute()
  }

  def main(args: Array[String]): Unit = {

    Range(0, 5).foreach { _ =>
      doTheThing(
        DetectorGenerationMethod(
          detectorRadiusMethod = NearestSelfSampleRadius(),
          redundancy = true,
          spatialPreference = false,
          featurePreference = false,
          borderProportion = 0.0,
          detectorRedundancyProportion = 0.8,
          detectorRedundancyTerminationThreshold = 10.0
        )
      )
    }

    /*

    val radiusMethods =
      //Range.BigDecimal(0.7, 1.0, 0.1).map(r => NearestSelfSampleRadius(r.doubleValue())) ++
      //Seq(NearestSelfSampleRadius())
      //Range.BigDecimal(0.015, 0.0151, 0.001).map(r => FixedRadius(r.doubleValue()))
      Seq(FixedRadius(0.1), FixedRadius(0.01))

    val redundancyProportions = Range.BigDecimal(0.75, 1.01, 0.01).map(_.doubleValue())
    val redundancyThresholds = Range.BigDecimal(0.1, 1.01, 0.1).map(_.doubleValue())

    radiusMethods.foreach { method =>
      redundancyProportions.foreach { proportion =>
        redundancyThresholds.foreach { threshold =>
          logger.error(s"Method: $method Proportion: $proportion Threshold: $threshold")

          doTheThing(
            DetectorGenerationMethod(
              detectorRadiusMethod = method,
              redundancy = true,
              spatialPreference = false,
              featurePreference = false,
              borderProportion = 0.0,
              detectorRedundancyProportion = proportion,
              detectorRedundancyTerminationThreshold = threshold
            )
          )
        }
      }
    }

     */
  }
}
