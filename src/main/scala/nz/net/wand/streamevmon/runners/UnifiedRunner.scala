package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.{DistDiffDetector, WindowedDistDiffDetector}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink._
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import java.time.Duration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.{GlobalWindow, TimeWindow, Window}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect._

object UnifiedRunner {

  implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[Measurement]] =
    TypeInformation.of(classOf[NormalDistribution[Measurement]])

  implicit class DataStreamExtensions(source: DataStream[Measurement]) {
    implicit def filterType[T <: Measurement: ClassTag]: DataStream[Measurement] = {
      source
        .filter(classTag[T].runtimeClass.isInstance(_))
        .name(s"Is ${classTag[T].runtimeClass.getSimpleName}?")
        .uid(s"filter-is-${classTag[T].runtimeClass.getSimpleName}")
    }

    implicit def notLossy[T <: Measurement: ClassTag]: DataStream[Measurement] = {
      source
        .filter(!_.isLossy)
        .name("Is not lossy?")
        .uid(s"filter-has-data-${classTag[T].getClass.getSimpleName}")
    }
  }

  implicit class KeyedStreamExtensions[K](source: KeyedStream[Measurement, K]) {
    implicit def addDetector[T <: KeyedProcessFunction[K, Measurement, Event]](
      detector: T,
      name    : String,
      uid     : String
    ): Unit = {
      val result =
        source
          .process(detector)
          .name(name)
          .uid(uid)
      detectors.append(result)
    }
  }

  implicit class WindowedStreamExtensions[K, W <: Window](source: WindowedStream[Measurement, K, W]) {
    implicit def addDetector[T <: ProcessWindowFunction[Measurement, Event, K, W]](
      detector: T,
      name    : String,
      uid     : String
    ): Unit = {
      val result =
        source
          .process(detector)
          .name(name)
          .uid(uid)
      detectors.append(result)
    }

    implicit def wrapAndAddDetector[T <: KeyedProcessFunction[String, Measurement, Event]](
      detector: T,
      name    : String,
      uid     : String
    ): Unit = {
      val wrapped = new WindowedFunctionWrapper[Measurement, TimeWindow](detector)
        .asInstanceOf[ProcessWindowFunction[Measurement, Event, K, W]]
      val result =
        source
          .process(wrapped)
          .name(s"$name (Window Wrapped)")
          .uid(s"window-wrapped-$uid")
      detectors.append(result)
    }
  }

  def isEnabled(env: StreamExecutionEnvironment, detectorName: String): Boolean = {
    env.getConfig.getGlobalJobParameters
      .asInstanceOf[ParameterTool]
      .getBoolean(s"detector.$detectorName.enabled")
  }

  val detectors: mutable.Buffer[DataStream[Event]] = mutable.Buffer()

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.amp.subscriptionName", "AmpUnifiedRunner")
    System.setProperty("influx.dataSource.bigdata.subscriptionName", "BigDataUnifiedRunner")

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    val keySelector = new MeasurementKeySelector[Measurement]

    lazy val ampMeasurementSource = env
      .addSource(new AmpMeasurementSourceFunction)
      .name("AMP Measurement Subscription")
      .uid("amp-measurement-source")

    lazy val icmpStream = ampMeasurementSource
      .filterType[ICMP]
      .notLossy[ICMP]
      .keyBy(keySelector)
    lazy val icmpStreamTimeWindow = icmpStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    lazy val dnsStream = ampMeasurementSource
      .filterType[DNS]
      .keyBy(keySelector)
    lazy val dnsStreamTimeWindow = dnsStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    lazy val bigdataMeasurementSource = env
      .addSource(new BigDataSourceFunction)
      .name("Libtrace-Bigdata Measurement Subscription")
      .uid("bigdata-measurement-source")

    lazy val flowStatisticStream = bigdataMeasurementSource
      .filterType[Flow]
      .keyBy(keySelector)
    lazy val flowStatisticStreamTimeWindow = flowStatisticStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    if (isEnabled(env, "changepoint")) {
      val changepoint = new ChangepointDetector[Measurement, NormalDistribution[Measurement]](
        new NormalDistribution(mean = 0)
      )
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(changepoint, changepoint.detectorName, changepoint.detectorUid)
      }
      else {
        icmpStream.addDetector(changepoint, changepoint.detectorName, "changepoint-detector")
      }
    }

    if (isEnabled(env, "distdiff")) {
      if (config.getBoolean("detector.distdiff.useFlinkWindow")) {
        val distDiffDetector = new WindowedDistDiffDetector[Measurement, GlobalWindow]
        icmpStream
          .countWindow(config.getInt("detector.distdiff.recentsCount") * 2, 1)
          .addDetector(distDiffDetector, distDiffDetector.detectorName, distDiffDetector.detectorUid)
      }
      else {
        val distDiffDetector = new DistDiffDetector[Measurement]
        icmpStream.addDetector(distDiffDetector, distDiffDetector.detectorName, distDiffDetector.detectorUid)
      }
    }

    if (isEnabled(env, "mode")) {
      val modeDetector = new ModeDetector[Measurement]
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(modeDetector, modeDetector.detectorName, modeDetector.detectorUid)
      }
      else {
        icmpStream.addDetector(modeDetector, modeDetector.detectorName, modeDetector.detectorUid)
      }
    }

    if (isEnabled(env, "loss")) {
      val lossDetector = new LossDetector[Measurement]
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        dnsStreamTimeWindow.wrapAndAddDetector(lossDetector, lossDetector.detectorName, lossDetector.detectorUid)
      }
      else {
        dnsStream.addDetector(lossDetector, lossDetector.detectorName, lossDetector.detectorUid)
      }
    }

    val allEvents = detectors.length match {
      case 0 => None
      case 1 => Some(detectors.head)
      // :_* just forces a sequence into the shape of varargs.
      case _ => Some(detectors.head.union(detectors.drop(1): _*))
    }

    val influxSink = new InfluxSinkFunction
    allEvents.map {
      _.addSink(influxSink)
        .setParallelism(1)
        .name("Influx Sink")
        .uid("influx-sink")
    }

    env.execute("Unified Runner")
  }
}
