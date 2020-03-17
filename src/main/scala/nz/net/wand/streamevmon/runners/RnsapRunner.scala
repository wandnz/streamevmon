package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Caching, Configuration, Logging}
import nz.net.wand.streamevmon.detectors.negativeselection._
import nz.net.wand.streamevmon.detectors.negativeselection.graphs.{DummyGraphs, RealGraphs, RnsapGraphs}
import nz.net.wand.streamevmon.detectors.negativeselection.DetectorGenerationMethod._
import nz.net.wand.streamevmon.detectors.negativeselection.TrainingDataSplitWindowAssigner.{TestingWindow, TrainingWindow}
import nz.net.wand.streamevmon.flink.{HabermanFileInputFormat, MeasurementKeySelector}
import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}
import nz.net.wand.streamevmon.measurements.SimpleIterableMeasurement

import java.time.Instant
import java.util
import java.util.concurrent.TimeUnit

import org.apache.commons.io.FilenameUtils
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.{environment, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner
import org.apache.flink.streaming.api.windowing.triggers.Trigger
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import scala.collection.JavaConverters._
import scala.io.Source

object RnsapRunner extends Logging with Caching {

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
        randomSeed = Some(42L),
        trainingSetNormalProportion = 1.0,
        testingSetNormalProportion = 0.1,
        testingSetAnomalousProportion = 1.0,
      ))
      .process(new RnsapDetector[Haberman, TimeWindow](
        method,
        maxDimensions = Int.MaxValue,
        graphs
      ))

    env.execute()
  }

  var trainCount: Option[Int] = None
  var testCount: Option[Int] = None

  def doTheThingCsv(
    method: DetectorGenerationMethod,
    maxDimensions: Int,
    graphs: RnsapGraphs,
    trainFilename: String,
    testFilename : String
  ): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("web.timeout", TimeUnit.MINUTES.toMillis(5).toString)
    val config = Configuration.get(Array())
    env.getConfig.setGlobalJobParameters(config)

    env.setParallelism(1)
    env.setMaxParallelism(1)

    // Subtract one for the column header
    if (trainCount.isEmpty) {
      val src = Source.fromFile(trainFilename)
      trainCount = Some(src.getLines().size - 1)
      src.close()
    }
    if (testCount.isEmpty) {
      val src = Source.fromFile(testFilename)
      testCount = Some(src.getLines().size - 1)
      src.close()
    }

    logger.debug(s"${trainCount.get} training elements, ${testCount.get} testing elements")

    env.addSource(new SourceFunction[SimpleIterableMeasurement] {
      var time = 1000000000000L

      override def run(ctx: SourceFunction.SourceContext[SimpleIterableMeasurement]): Unit = {
        collectAllLines(trainFilename, 0, ctx)
        val sentTrainCount = time - 1000000000000L
        if (sentTrainCount != trainCount.get) {
          logger.error(s"Sent $sentTrainCount elements, expected ${trainCount.get}!!!")
        }
        logger.info("Sent all training data")
        ctx.emitWatermark(new Watermark(time))
        time += 1
        ctx.markAsTemporarilyIdle()

        Thread.sleep(5000)

        collectAllLines(testFilename, 1, ctx)
        if (time - 1000000000000L - sentTrainCount != testCount.get) {
          logger.error(s"Sent ${time - 1000000000000L - sentTrainCount} elements, expected ${testCount.get}!!!")
        }
        logger.info("Sent all testing data")
        ctx.emitWatermark(new Watermark(time))
        time += 1
        ctx.markAsTemporarilyIdle()
      }

      private def collectAllLines(
        filename: String,
        stream: Int,
        ctx: SourceFunction.SourceContext[SimpleIterableMeasurement]
      ): Unit = {
        val file: Option[Seq[String]] = getWithCache(
          filename,
          None,
          {
            val s = Source.fromFile(filename)
            val l = s.getLines().toList
            s.close()
            Some(l)
          }
        )

        file.get.foreach { line =>
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
            trainCount.get,
            testCount.get
          )
        }

        override def getWindowSerializer(executionConfig: ExecutionConfig): TypeSerializer[TimeWindow] = new TimeWindow.Serializer()

        override def isEventTime: Boolean = true
      })
      .process(new RnsapDetector[SimpleIterableMeasurement, TimeWindow](
        method,
        maxDimensions = maxDimensions,
        graphs
      ))

    env.execute()
  }

  def testHaberman(): Unit = {
    val dataToTest = Seq(("Haberman", ""))

    val radiusMethods = Seq(NearestSelfSampleRadius())
    val redundancyProportions = Seq(0.8)
    val redundancyThresholds = Seq(1.0, 2.0, 5.0, 10.0, 15.0, 20.0, 30.0)
    val borderProportions = Seq(0.1, 0.0)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")

      val graphs = new RealGraphs(
        graphFilename = None,
        csvFilename = Some(s"./out/csv/rnsap-${FilenameUtils.getBaseName(files._1)}")
      )
      graphs.initCsv()

      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          borderProportions.foreach { border =>
            redundancyThresholds.foreach { threshold =>
              Seq(true, false).foreach { backfilter =>
                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 100).foreach { reps =>
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
              }
            }
          }
        }
      }
    }
  }

  def testUnsw8(): Unit = {
    val dataToTest = Seq(
      ("data/unsw/UNSW_NB15_training-set-numeralised-8-features.csv", "data/unsw/UNSW_NB15_testing-set-numeralised-8-features.csv")
    )

    val radiusMethods = Seq(NearestSelfSampleRadius())
    val redundancyProportions = Seq(0.6)
    val redundancyThresholds = Seq(20.0, 30.0)
    val borderProportions = Seq(0.1) //Seq(0.1, 0.0)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")

      val graphs = new RealGraphs(
        graphFilename = None,
        csvFilename = Some(s"./out/csv/rnsap-${FilenameUtils.getBaseName(files._1)} (0.1 outer_radius, 20 and up R)")
      )
      graphs.initCsv()

      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          borderProportions.foreach { border =>
            redundancyThresholds.foreach { threshold =>
              Seq(true, false).foreach { backfilter =>
                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 100).foreach { reps =>
                  doTheThingCsv(
                    DetectorGenerationMethod(
                      detectorRadiusMethod = method,
                      backfiltering = backfilter,
                      borderProportion = border,
                      detectorRedundancyProportion = proportion,
                      detectorRedundancyTerminationThreshold = threshold
                    ),
                    Int.MaxValue,
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

  def testUnsw5(): Unit = {

    val dataToTest = Seq(
      ("data/unsw/UNSW_NB15_training-set-numeralised-5-features.csv", "data/unsw/UNSW_NB15_testing-set-numeralised-5-features.csv")
    )

    val radiusMethods = Seq(NearestSelfSampleRadius())
    val redundancyProportions = Seq(0.6)
    val redundancyThresholds = Seq(1.0, 2.0, 5.0, 10.0, 15.0, 20.0, 30.0)
    val borderProportions = Seq(0.1, 0.0)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")

      val graphs = new RealGraphs(
        graphFilename = None,
        csvFilename = Some(s"./out/csv/rnsap-${FilenameUtils.getBaseName(files._1)}")
      )
      graphs.initCsv()

      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          borderProportions.foreach { border =>
            redundancyThresholds.foreach { threshold =>
              Seq(true, false).foreach { backfilter =>
                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 100).foreach { reps =>
                  doTheThingCsv(
                    DetectorGenerationMethod(
                      detectorRadiusMethod = method,
                      backfiltering = backfilter,
                      borderProportion = border,
                      detectorRedundancyProportion = proportion,
                      detectorRedundancyTerminationThreshold = threshold
                    ),
                    Int.MaxValue,
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

  def testKddAll(): Unit = {
  }

  def testKdd11(): Unit = {
    val dataToTest = Seq(
      ("data/kdd-nsl/KDDTrain+numeralised-11-features.csv", "data/kdd-nsl/KDDTest+numeralised-11-features.csv")
    )

    val radiusMethods = Seq(NearestSelfSampleRadius())
    val redundancyProportions = Seq(0.6, 0.8)
    val redundancyThresholds = Seq(10.0, 15.0, 20.0, 30.0)
    val borderProportions = Seq(0.0) //Seq(0.1, 0.0)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")

      val graphs = new RealGraphs(
        graphFilename = None,
        csvFilename = Some(s"./out/csv/rnsap-${FilenameUtils.getBaseName(files._1)} (0.0 outer_radius only)")
      )
      graphs.initCsv()

      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          borderProportions.foreach { border =>
            redundancyThresholds.foreach { threshold =>
              Seq(true, false).foreach { backfilter =>
                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 100).foreach { reps =>
                  doTheThingCsv(
                    DetectorGenerationMethod(
                      detectorRadiusMethod = method,
                      backfiltering = backfilter,
                      borderProportion = border,
                      detectorRedundancyProportion = proportion,
                      detectorRedundancyTerminationThreshold = threshold
                    ),
                    Int.MaxValue,
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

  def testPostBackfiltering(): Unit = {
    doTheThingHaberman(
      DetectorGenerationMethod(
        detectorRadiusMethod = NearestSelfSampleRadius(),
        backfiltering = false,
        postBackfiltering = true,
        borderProportion = 0.1,
        detectorRedundancyProportion = 0.8,
        detectorRedundancyTerminationThreshold = 15.0
      ), new DummyGraphs
    )
  }

  def main(args: Array[String]): Unit = {

    val dataToTest = Seq(
      //("Haberman", ""),
      //("data/unsw-normal/UNSW_training_set_normalized.csv", "data/unsw-normal/UNSW_test_set_normalized.csv"),
      //("data/unsw/UNSW_NB15_training-set-numeralised.csv", "data/unsw/UNSW_NB15_testing-set-numeralised.csv"),
      //("data/unsw/UNSW_NB15_training-set-numeralised-8-features.csv", "data/unsw/UNSW_NB15_testing-set-numeralised-8-features.csv"),
      //("data/unsw/UNSW_NB15_training-set-numeralised-5-features.csv", "data/unsw/UNSW_NB15_testing-set-numeralised-5-features.csv"),
      ("data/kdd-nsl/KDDTrain+numeralised.csv", "data/kdd-nsl/KDDTest+numeralised.csv"),
    )

    val radiusMethods = Seq(NearestSelfSampleRadius())
    //val redundancyProportions = Range.BigDecimal(0.60, 0.199, -0.1).map(_.doubleValue())
    val redundancyProportions = Seq(0.7, 0.8, 0.9, 1.0).reverse
    val redundancyThresholds = Seq(1.0, 2.0, 5.0, 10.0, 15.0, 20.0, 30.0)
    //val redundancyThresholds = Seq(3.0)
    val borderProportions = Seq(0.1, 0.2, 0.0)

    dataToTest.foreach { files =>
      logger.info(s"Testing on $files")

      val graphs = new RealGraphs(
        graphFilename = None,
        csvFilename = Some(s"./out/csv/rnsap-${FilenameUtils.getBaseName(files._1)}")
      )
      graphs.initCsv()

      radiusMethods.foreach { method =>
        redundancyProportions.foreach { proportion =>
          borderProportions.foreach { border =>
            redundancyThresholds.foreach { threshold =>
              Seq(true, false).foreach { backfilter =>
                logger.info(s"$method, proportion=$proportion, threshold=$threshold, border=$border, backfilter=$backfilter")

                Range(0, 100).foreach { reps =>
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
                      Int.MaxValue,
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
