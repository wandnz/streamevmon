package nz.net.wand.streamevmon.detectors

import nz.net.wand.streamevmon.events.ChangepointEvent
import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.reflect._

class ChangepointDetector[T <: Measurement: ClassTag]
    extends KeyedProcessFunction[Int, T, ChangepointEvent] {

  final val detectorName = s"Changepoint Detector (${classTag[T].runtimeClass.getSimpleName})"

  override def processElement(value: T,
                              ctx: KeyedProcessFunction[Int, T, ChangepointEvent]#Context,
                              out: Collector[ChangepointEvent]): Unit = {}

}
