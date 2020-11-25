package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.Lazy
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow
import nz.net.wand.streamevmon.measurements.esmond._
import nz.net.wand.streamevmon.measurements.latencyts._
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.measurements.traits.Measurement

import java.util.NoSuchElementException

import org.apache.flink.streaming.api.scala.DataStream

import scala.reflect._

/** Lazily filters DataStreams by Measurement subtype. */
case class StreamToTypedStreams(
  private val rawStream: Lazy[DataStream[Measurement]],
  sourceInstance       : SourceInstance
) {

  /** Applies the filter operation, and names it. */
  private def getTypedAs[MeasT <: Measurement : ClassTag]: TypedStreams = {
    TypedStreams(
      new Lazy(rawStream.get
        .filter(classTag[MeasT].runtimeClass.isInstance(_))
        .name(s"Is ${classTag[MeasT].runtimeClass.getSimpleName}?")
      )
    )
  }

  /** Maps the given SourceDatatype to a TypedStreams, which includes further filters.
    *
    * Throws IllegalArgumentException if trying to filter by a type which is
    * not supported by the source stream.
    */
  lazy val typedAs: Map[SourceDatatype.Value, TypedStreams] = {
    // We construct a map that only consists of types supported by the source stream.
    val supportedTypesMap: Map[SourceDatatype.Value, TypedStreams] = sourceInstance.sourceType match {
      case SourceType.Influx => sourceInstance.sourceSubtype match {
        case _@Some(SourceSubtype.Amp) => SourceDatatype.values.flatMap {
          case d@SourceDatatype.DNS => Some((d, getTypedAs[DNS]))
          case d@SourceDatatype.HTTP => Some((d, getTypedAs[HTTP]))
          case d@SourceDatatype.ICMP => Some((d, getTypedAs[ICMP]))
          case d@SourceDatatype.TCPPing => Some((d, getTypedAs[TCPPing]))
          case d@SourceDatatype.TraceroutePathlen => Some((d, getTypedAs[TraceroutePathlen]))
          case _ => None
        }.toMap
        case _@Some(SourceSubtype.Bigdata) => SourceDatatype.values.flatMap {
          case d@SourceDatatype.Flow => Some((d, getTypedAs[Flow]))
          case _ => None
        }.toMap
        case sub => throw new IllegalArgumentException(s"Invalid subtype $sub for source type ${sourceInstance.sourceType}!")
      }
      case SourceType.Esmond => SourceDatatype.values.flatMap {
        case d@SourceDatatype.Failure => Some((d, getTypedAs[Failure]))
        case d@SourceDatatype.Histogram => Some((d, getTypedAs[Histogram]))
        case d@SourceDatatype.Href => Some((d, getTypedAs[Href]))
        case d@SourceDatatype.PacketTrace => Some((d, getTypedAs[PacketTrace]))
        case d@SourceDatatype.Simple => Some((d, getTypedAs[Simple]))
        case d@SourceDatatype.Subinterval => Some((d, getTypedAs[Subinterval]))
        case _ => None
      }.toMap
      case SourceType.LatencyTS => SourceDatatype.values.flatMap {
        case d@SourceDatatype.LatencyTSAmp => Some((d, getTypedAs[LatencyTSAmpICMP]))
        case d@SourceDatatype.LatencyTSSmokeping => Some((d, getTypedAs[LatencyTSSmokeping]))
        case _ => None
      }.toMap
      case SourceType.NAB => SourceDatatype.values.flatMap {
        case d@SourceDatatype.NAB => Some((d, getTypedAs[NabMeasurement]))
        case _ => None
      }.toMap
      case _ => Map()
    }

    /** Thin wrapper around whatever Map type gets returned earlier, to make the
      * exception thrown when a nonexistent element is accessed more relevant.
      */
    class MapWithNicerErrorMessage[K, V](
      private val internal: Map[K, V],
      private val sourceInstance: SourceInstance
    ) extends Map[K, V] {
      override def +[V1 >: V](kv: (K, V1)): Map[K, V1] = internal + kv

      override def get(key: K): Option[V] = internal.get(key)

      override def iterator: Iterator[(K, V)] = internal.iterator

      override def -(key: K): Map[K, V] = internal - key

      override def apply(key: K): V = {
        try {
          super.apply(key)
        }
        catch {
          case _: NoSuchElementException => throw new IllegalArgumentException(
            s"Source with type ${sourceInstance.sourceType} " +
              sourceInstance.sourceSubtype.map(s => s"and subtype $s ").getOrElse("") +
              s"does not support measurements of type $key!"
          )
        }
      }
    }

    new MapWithNicerErrorMessage(supportedTypesMap, sourceInstance)
  }
}
