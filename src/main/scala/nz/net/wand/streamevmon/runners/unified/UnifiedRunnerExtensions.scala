package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{MeasurementKeySelector, WindowedFunctionWrapper}
import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, Measurement}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.{GlobalWindow, TimeWindow, Window}

import scala.collection.mutable
import scala.language.{higherKinds, implicitConversions}
import scala.reflect._

/** Defines a number of extension methods for use in [[UnifiedRunner]]. */
trait UnifiedRunnerExtensions {
  var config: ParameterTool = _

  val detectors: mutable.Buffer[DataStream[Event]] = mutable.Buffer()

  lazy val keySelector: MeasurementKeySelector[InfluxMeasurement] = new MeasurementKeySelector[InfluxMeasurement]

  def isEnabled(detectorName: String): Boolean = {
    config.getBoolean(s"detector.$detectorName.enabled")
  }

  /** Container for a keyed stream and a windowed stream. */
  case class KeyedAndWindowedStreams[MeasT <: Measurement : ClassTag, W <: Window](
    keyed   : KeyedStream[MeasT, String],
    windowed: WindowedStream[MeasT, String, W]
  ) {
    /** Adds a detector to the appropriate stream depending on if windowing is enabled. */
    def addDetector[T <: KeyedProcessFunction[String, MeasT, Event] with HasFlinkConfig](
      keyedDetector: T
    ): Option[DataStream[Event]] = {
      if (isEnabled(keyedDetector.configKeyGroup)) {
        if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
          Some(windowed.wrapAndAddDetector(keyedDetector))
        }
        else {
          Some(keyed.addDetector(keyedDetector))
        }
      }
      else {
        None
      }
    }

    /** Adds a detector to the appropriate stream depending on if windowing is enabled.
      * This version supports detectors which have a different type to support windowing manually.
      */
    def addDetector[KeyedT <: KeyedProcessFunction[String, MeasT, Event] with HasFlinkConfig,
      WindowedT <: ProcessWindowFunction[MeasT, Event, String, W] with HasFlinkConfig](
      keyedDetector   : KeyedT,
      windowedDetector: WindowedT
    ): Option[DataStream[Event]] = {
      if (isEnabled(windowedDetector.configKeyGroup)) {
        if (config.getBoolean(s"detector.${windowedDetector.configKeyGroup}.useFlinkWindow")) {
          Some(windowed.addDetector(windowedDetector))
        }
        else {
          Some(keyed.addDetector(keyedDetector))
        }
      }
      else {
        None
      }
    }
  }

  implicit class DataStreamExtensions[MeasT <: Measurement : ClassTag](source: DataStream[MeasT]) {
    /** Filters a DataStream to only include a certain type. */
    implicit def filterType[T <: MeasT : ClassTag]: DataStream[MeasT] = {
      source
        .filter(classTag[T].runtimeClass.isInstance(_))
        .name(s"Is ${classTag[T].runtimeClass.getSimpleName}?")
        .uid(s"filter-is-${classTag[T].runtimeClass.getSimpleName}")
    }

    /** Filters a DataStream to only include Measurements that aren't lossy. */
    implicit def notLossy[T <: MeasT : ClassTag]: DataStream[MeasT] = {
      source
        .filter((m: MeasT) => !m.isLossy)
        .name("Is not lossy?")
        .uid(s"filter-has-data-${classTag[T].getClass.getSimpleName}")
    }

    /** Create a keyed stream and a time-window stream from an input stream. */
    implicit def keyAndTimeWindowStreams[T <: MeasT : ClassTag]: KeyedAndWindowedStreams[MeasT, TimeWindow] = {
      val keyedStream = source.keyBy(new MeasurementKeySelector[MeasT])
      KeyedAndWindowedStreams(
        keyedStream,
        // TODO: This should use detector-specific window duration configs
        keyedStream.timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))
      )
    }

    /** Create a keyed stream and a count-window stream from an input stream. */
    implicit def keyAndCountWindowStreams[T <: MeasT : ClassTag](windowSize: Long, slide: Long): KeyedAndWindowedStreams[MeasT, GlobalWindow] = {
      val keyedStream = source.keyBy(new MeasurementKeySelector[MeasT])
      KeyedAndWindowedStreams(
        keyedStream,
        keyedStream.countWindow(windowSize, slide)
      )
    }
  }

  implicit class KeyedStreamExtensions[MeasT <: Measurement, KeyT](source: KeyedStream[MeasT, KeyT]) {
    /** Adds a detector to a KeyedStream, and our list of existing detectors. */
    implicit def addDetector[T <: KeyedProcessFunction[KeyT, MeasT, Event] with HasFlinkConfig](
      detector: T
    ): DataStream[Event] = {
      val result =
        source
          .process(detector)
          .name(detector.flinkName)
          .uid(detector.flinkUid)
      detectors.append(result)
      result
    }
  }

  implicit class WindowedStreamExtensions[MeasT <: Measurement : ClassTag, W <: Window](source: WindowedStream[MeasT, String, W]) {
    /** Adds a detector to a WindowedStream, and our list of existing detectors. */
    implicit def addDetector[T <: ProcessWindowFunction[MeasT, Event, String, W] with HasFlinkConfig](
      detector: T
    ): DataStream[Event] = {
      val result =
        source
          .process(detector)
          .name(detector.flinkName)
          .uid(detector.flinkUid)
      detectors.append(result)
      result
    }

    /** Wraps a detector to turn it into a windowed function, and adds it. */
    implicit def wrapAndAddDetector[T <: KeyedProcessFunction[String, MeasT, Event] with HasFlinkConfig](
      detector: T
    ): DataStream[Event] = {
      val wrapped = new WindowedFunctionWrapper[MeasT, TimeWindow](detector)
        .asInstanceOf[ProcessWindowFunction[MeasT, Event, String, W]]
      val result =
        source
          .process(wrapped)
          .name(s"${detector.flinkName} (Window Wrapped)")
          .uid(s"window-wrapped-${detector.flinkUid}")
      detectors.append(result)
      result
    }
  }
}
