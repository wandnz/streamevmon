package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import org.apache.flink.streaming.api.scala.{DataStream, KeyedStream, _}

class Lazy[A](operation: => A) {
  lazy val get: A = operation
}

case class SourceAndFilters(
  rawStream: Lazy[DataStream[Measurement]]
) {
  lazy val typedAs: Map[SourceReferenceDatatype.Value, TypedStreams] =
    SourceReferenceDatatype.values.map {
      case d@SourceReferenceDatatype.DNS =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[DNS])
          .name("Is DNS?")
        )))
      case d@SourceReferenceDatatype.HTTP =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[HTTP])
          .name("Is HTTP?")
        )))
      case d@SourceReferenceDatatype.ICMP =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[ICMP])
          .name("Is ICMP?")
        )))
      case d@SourceReferenceDatatype.TCPPing =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[TCPPing])
          .name("Is TCPPing?")
        )))
      case d@SourceReferenceDatatype.Traceroute =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Traceroute])
          .name("Is Traceroute?")
        )))
      case d@SourceReferenceDatatype.Flow =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Flow])
          .name("Is Flow?")
        )))
    }.toMap
}

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
