package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Lazy

import org.apache.flink.streaming.api.scala._

/** Contains a type-filtered stream, as well as keyed and non-lossy variants. */
case class TypedStreams(
  typedStream: Lazy[DataStream[Measurement]]
) {
  lazy val notLossy: DataStream[Measurement] = typedStream.get
    .filter(!_.isLossy)
    .name("Is not lossy?")
  lazy val keyedStream: KeyedStream[Measurement, String] = typedStream.get
    .keyBy(new MeasurementKeySelector[Measurement])
  lazy val notLossyKeyedStream: KeyedStream[Measurement, String] = notLossy
    .keyBy(new MeasurementKeySelector[Measurement])
}
