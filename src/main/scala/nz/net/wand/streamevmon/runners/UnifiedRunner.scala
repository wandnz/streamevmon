package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.MapFunction
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{InfluxSinkFunction, MeasurementKeySelector, MeasurementSourceFunction}
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._

import java.time.Duration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect._

object UnifiedRunner {

  implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[Measurement]] =
    TypeInformation.of(classOf[NormalDistribution[Measurement]])

  class IcmpToMedian() extends MapFunction[Measurement, Double] with Serializable {
    override def apply(t: Measurement): Double = t.asInstanceOf[ICMP].median.get
  }

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
        name: String,
        uid: String
    ): Unit = {
      val result =
        source
          .process(detector)
          .name(name)
          .uid(uid)
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

    System.setProperty("influx.dataSource.subscriptionName", "UnifiedRunner")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    env.enableCheckpointing(Duration.ofSeconds(10).toMillis, CheckpointingMode.EXACTLY_ONCE)

    val measurementSource = env
      .addSource(new MeasurementSourceFunction)
      .name("Measurement Subscription")
      .uid("measurement-source")

    val keySelector = new MeasurementKeySelector[Measurement]

    val icmpStream = measurementSource
      .filterType[ICMP]
      .notLossy[ICMP]
      .keyBy(keySelector)

    val dnsStream = measurementSource
      .filterType[DNS]
      .keyBy(keySelector)

    if (isEnabled(env, "changepoint")) {
      val changepoint = new ChangepointDetector[Measurement, NormalDistribution[Measurement]](
        new NormalDistribution(mean = 0, mapFunction = new IcmpToMedian)
      )
      icmpStream.addDetector(changepoint, changepoint.detectorName, "changepoint-detector")
    }

    if (isEnabled(env, "mode")) {
      val modeDetector = new ModeDetector[Measurement]
      icmpStream.addDetector(modeDetector, modeDetector.detectorName, "mode-detector")
    }

    if (isEnabled(env, "loss")) {
      val lossDetector = new LossDetector[Measurement]
      dnsStream.addDetector(lossDetector, lossDetector.detectorName, "loss-detector")
    }

    val allEvents = detectors.length match {
      case 0 => None
      case 1 => Some(detectors.head)
      // :_* just forces a sequence into the shape of varags.
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
