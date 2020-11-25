package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata._
import nz.net.wand.streamevmon.measurements.esmond._
import nz.net.wand.streamevmon.measurements.latencyts._
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.measurements.traits.Measurement

import org.apache.flink.api.java.functions.KeySelector

import scala.reflect.ClassTag

/** Used to convert a DataStream to a KeyedStream. Stream ID is based on the
  * type and `stream` field of the measurement. This means that items sharing a
  * value for `stream`, but with a different concrete type will usually not be
  * sent to the same downstream operators.
  */
class MeasurementKeySelector[T <: Measurement : ClassTag] extends KeySelector[T, String] {
  override def getKey(value: T): String =
    value match {
      case m@(_: DNS | _: RichDNS) => s"DNS-${m.stream}"
      case m@(_: HTTP | _: RichHTTP) => s"HTTP-${m.stream}"
      case m@(_: ICMP | _: RichICMP) => s"ICMP-${m.stream}"
      case m@(_: TCPPing | _: RichTCPPing) => s"TCPPing-${m.stream}"
      case m@(_: TraceroutePathlen | _: RichTraceroutePathlen) => s"TraceroutePathlen-${m.stream}"
      case m@(_: LatencyTSAmpICMP) => s"LatencyTSAmpICMP-${m.stream}"
      case m@(_: LatencyTSSmokeping) => s"LatencyTSSmokeping-${m.stream}"
      case m@(_: Flow) => s"Flow-${m.stream}"
      case m@(_: EsmondMeasurement) => s"esmond-${m.stream}"
      case m@(_: NabMeasurement) => s"nab-${m.stream}"
      case m => throw new IllegalArgumentException(s"Unknown measurement type ${m.getClass.getSimpleName}")
    }
}
