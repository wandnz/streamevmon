package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.MeasurementKeySelector
import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.esmond._

import org.apache.flink.streaming.api.scala._

class Lazy[A](operation: => A) {
  lazy val get: A = operation
}

case class SourceAndFilters(
  rawStream: Lazy[DataStream[Measurement]]
) {
  lazy val typedAs: Map[SourceReferenceDatatype.Value, TypedStreams] =
    SourceReferenceDatatype.values.map {
      // AMP
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
      // Bigdata
      case d@SourceReferenceDatatype.Flow =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Flow])
          .name("Is Flow?")
        )))
      // Esmond
      case d@SourceReferenceDatatype.Failure =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Failure])
          .name("Is Failure?")
        )))
      case d@SourceReferenceDatatype.Histogram =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Histogram])
          .name("Is Histogram?")
        )))
      case d@SourceReferenceDatatype.Href =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Href])
          .name("Is Href?")
        )))
      case d@SourceReferenceDatatype.PacketTrace =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[PacketTrace])
          .name("Is PacketTrace?")
        )))
      case d@SourceReferenceDatatype.Simple =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Simple])
          .name("Is Simple?")
        )))
      case d@SourceReferenceDatatype.Subinterval =>
        (d, TypedStreams(new Lazy(rawStream.get
          .filter(_.isInstanceOf[Subinterval])
          .name("Is Subinterval?")
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
