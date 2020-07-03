package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.detectors.HasNameAndUid
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{MeasurementKeySelector, WindowedFunctionWrapper}
import nz.net.wand.streamevmon.measurements.{InfluxMeasurement, Measurement}

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.{TimeWindow, Window}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect._

/** Defines a number of extension methods for use in [[UnifiedRunner]].
  */
trait UnifiedRunnerExtensions {

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
  }

  implicit class KeyedStreamExtensions[MeasT <: Measurement, KeyT](source: KeyedStream[MeasT, KeyT]) {
    /** Adds a detector to a KeyedStream, and our list of existing detectors. */
    implicit def addDetector[T <: KeyedProcessFunction[KeyT, MeasT, Event] with HasNameAndUid](
      detector: T
    ): Unit = {
      val result =
        source
          .process(detector)
          .name(detector.detectorName)
          .uid(detector.detectorUid)
      detectors.append(result)
    }
  }

  implicit class WindowedStreamExtensions[MeasT <: Measurement : ClassTag, W <: Window](source: WindowedStream[MeasT, String, W]) {
    /** Adds a detector to a non-keyed WindowedStream, and our list of existing detectors. */
    implicit def addDetector[T <: ProcessWindowFunction[MeasT, Event, String, W] with HasNameAndUid](
      detector: T
    ): Unit = {
      val result =
        source
          .process(detector)
          .name(detector.detectorName)
          .uid(detector.detectorUid)
      detectors.append(result)
    }

    implicit def wrapAndAddDetector[T <: KeyedProcessFunction[String, MeasT, Event] with HasNameAndUid](
      detector: T
    ): Unit = {
      val wrapped = new WindowedFunctionWrapper[MeasT, TimeWindow](detector)
        .asInstanceOf[ProcessWindowFunction[MeasT, Event, String, W]]
      val result =
        source
          .process(wrapped)
          .name(s"${detector.detectorName} (Window Wrapped)")
          .name(s"window-wrapped-${detector.detectorUid}")
      detectors.append(result)
    }
  }

  def isEnabled(env: StreamExecutionEnvironment, detectorName: String): Boolean = {
    env.getConfig.getGlobalJobParameters
      .asInstanceOf[ParameterTool]
      .getBoolean(s"detector.$detectorName.enabled")
  }

  val detectors: mutable.Buffer[DataStream[Event]] = mutable.Buffer()

  lazy val keySelector = new MeasurementKeySelector[InfluxMeasurement]
}
