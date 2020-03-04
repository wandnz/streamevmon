package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.detectors.negativeselection._
import nz.net.wand.streamevmon.detectors.negativeselection.graphs.{RealGraphs, RnsapGraphs}
import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod._
import nz.net.wand.streamevmon.detectors.negativeselection.TrainingDataSplitWindowAssigner.{TestingWindow, TrainingWindow}
import nz.net.wand.streamevmon.flink.{HabermanFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}
import nz.net.wand.streamevmon.measurements.SimpleIterableMeasurement

import java.time.Instant
import java.util
import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.{environment, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner
import org.apache.flink.streaming.api.windowing.triggers.Trigger
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.collection.JavaConverters._
import scala.io.Source

object RnsapRunner extends Logging {

  def doTheThingHaberman(
    method: DetectorGenerationMethod,
    graphs: RnsapGraphs
  ): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("web.timeout", TimeUnit.HOURS.toMillis(10).toString)
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

    env
      .readFile(format, filename)
      .assignAscendingTimestamps(_.time.toEpochMilli)
      .keyBy(new MeasurementKeySelector[Haberman])
      .window(new TrainingDataSplitWindowAssigner[Haberman](
        normalCount,
        anomalousCount,
        randomSeed = Some(42L)
      ))
      .process(new RnsapDetector[Haberman, TimeWindow](
        method,
        maxDimensions = Int.MaxValue,
        graphs
      ))

    env.execute()
  }

  def doTheThingCsv(
    method: DetectorGenerationMethod,
    graphs: RnsapGraphs,
    trainFilename: String,
    testFilename: String
  ): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("web.timeout", TimeUnit.HOURS.toMillis(10).toString)
    val config = Configuration.get(Array())
    env.getConfig.setGlobalJobParameters(config)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    // Subtract one for the column header
    val trainCount = {
      val src = Source.fromFile(trainFilename)
      val cnt = src.getLines().size - 1
      src.close()
      cnt
    }
    val testCount = {
      val src = Source.fromFile(testFilename)
      val cnt = src.getLines().size - 1
      src.close()
      cnt
    }

    logger.debug(s"$trainCount training elements, $testCount testing elements")

    env.addSource(new SourceFunction[SimpleIterableMeasurement] {
      var time = 1000000000000L

      override def run(ctx: SourceFunction.SourceContext[SimpleIterableMeasurement]): Unit = {
        collectAllLines(trainFilename, 0, ctx)
        val sentTrainCount = time - 1000000000000L
        if (sentTrainCount != trainCount) {
          logger.error(s"Sent $sentTrainCount elements, expected $trainCount!!!")
        }
        collectAllLines(testFilename, 1, ctx)
        if (time - 1000000000000L - sentTrainCount != testCount) {
          logger.error(s"Sent ${time - 1000000000000L - sentTrainCount} elements, expected $testCount!!!")
        }
      }

      private def collectAllLines(
        filename: String,
        stream: Int,
        ctx: SourceFunction.SourceContext[SimpleIterableMeasurement]
      ): Unit = {
        val file = Source.fromFile(filename)

        file.getLines().foreach { line =>
          val split = line.split(",")
          try {
            ctx.collectWithTimestamp(
              SimpleIterableMeasurement(
                stream,
                Instant.ofEpochMilli(time),
                split.map(_.toDouble)
              ),
              time
            )
            time += 1
          }
          catch {
            case _: NumberFormatException =>
          }
        }
        file.close()
      }

      override def cancel(): Unit = {}
    })
      .keyBy(_ => "0")
      .window(new WindowAssigner[SimpleIterableMeasurement, TimeWindow] {
        private def trainingWindow: TimeWindow = new TrainingWindow(0, Long.MaxValue - 1)

        private def testingWindow: TimeWindow = new TestingWindow(1, Long.MaxValue)

        override def assignWindows(
          element                         : SimpleIterableMeasurement,
          timestamp                       : Long,
          context                         : WindowAssigner.WindowAssignerContext
        ): util.Collection[TimeWindow] = {
          if (element.stream == 0) {
            Seq(trainingWindow).asJavaCollection
          }
          else {
            Seq(testingWindow).asJavaCollection
          }
        }

        override def getDefaultTrigger(env: environment.StreamExecutionEnvironment): Trigger[SimpleIterableMeasurement, TimeWindow] = {
          new DualWindowCountTrigger[SimpleIterableMeasurement, TimeWindow, TrainingWindow, TestingWindow](
            trainCount,
            testCount
          )
        }

        override def getWindowSerializer(executionConfig: ExecutionConfig): TypeSerializer[TimeWindow] = new TimeWindow.Serializer()

        override def isEventTime: Boolean = true
      })
      .process(new RnsapDetector[SimpleIterableMeasurement, TimeWindow](
        method,
        maxDimensions = Int.MaxValue,
        graphs
      ))

    env.execute()
  }

  def main(args: Array[String]): Unit = {

    val graphs = new RealGraphs

    graphs.initCsv()

    val dataToTest = Seq(
      //("Haberman", ""),
      ("data/unsw-normal/UNSW_training_set_normalized.csv", "data/unsw-normal/UNSW_test_set_normalized.csv"),
      //("data/kdd-normal/KDD_train_normalized.csv", "data/kdd-normal/KDD_test_normalized.csv")
    )

    val radiusMethods = Seq(NearestSelfSampleRadius())
    val redundancyProportions = Seq(0.80)
    //val redundancyThresholds = Seq(0.1, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 15.0, 20.0, 30.0)
    val redundancyThresholds = Seq(10.0)
    val borderProportions = Seq(0.1)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")
      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          redundancyThresholds.foreach { threshold =>
            borderProportions.foreach { border =>
              Seq(true).foreach { backfilter =>

                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 1).foreach { reps =>
                  if (files._1 == "Haberman") {
                    doTheThingHaberman(
                      DetectorGenerationMethod(
                        detectorRadiusMethod = method,
                        backfiltering = backfilter,
                        borderProportion = border,
                        detectorRedundancyProportion = proportion,
                        detectorRedundancyTerminationThreshold = threshold
                      ), graphs
                    )
                  }
                  else {
                    doTheThingCsv(
                      DetectorGenerationMethod(
                        detectorRadiusMethod = method,
                        backfiltering = backfilter,
                        borderProportion = border,
                        detectorRedundancyProportion = proportion,
                        detectorRedundancyTerminationThreshold = threshold
                      ),
                      graphs,
                      files._1,
                      files._2
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
