package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.Lazy
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.esmond._

import org.apache.flink.streaming.api.scala.DataStream

import scala.reflect._

/** Lazily filters DataStreams by Measurement subtype. */
case class StreamToTypedStreams(
  private val rawStream: Lazy[DataStream[Measurement]]
) {

  /** Applies the filter operation, and names it. */
  private def getTypedAs[MeasT <: Measurement : ClassTag](d: SourceDatatype.Value): (SourceDatatype.Value, TypedStreams) = {
    (d, TypedStreams(new Lazy(rawStream.get
      .filter(classTag[MeasT].runtimeClass.isInstance(_))
      .name(s"Is ${classTag[MeasT].runtimeClass.getSimpleName}?")
    )))
  }

  /** Maps the given SourceDatatype to a TypedStreams, which includes further filters. */
  lazy val typedAs: Map[SourceDatatype.Value, TypedStreams] =
    SourceDatatype.values.map {
      // AMP
      case d@SourceDatatype.DNS => getTypedAs[DNS](d)
      case d@SourceDatatype.HTTP => getTypedAs[HTTP](d)
      case d@SourceDatatype.ICMP => getTypedAs[ICMP](d)
      case d@SourceDatatype.TCPPing => getTypedAs[TCPPing](d)
      case d@SourceDatatype.Traceroute => getTypedAs[Traceroute](d)
      // Bigdata
      case d@SourceDatatype.Flow => getTypedAs[Flow](d)
      // Esmond
      case d@SourceDatatype.Failure => getTypedAs[Failure](d)
      case d@SourceDatatype.Histogram => getTypedAs[Histogram](d)
      case d@SourceDatatype.Href => getTypedAs[Href](d)
      case d@SourceDatatype.PacketTrace => getTypedAs[PacketTrace](d)
      case d@SourceDatatype.Simple => getTypedAs[Simple](d)
      case d@SourceDatatype.Subinterval => getTypedAs[Subinterval](d)
    }.toMap
}
