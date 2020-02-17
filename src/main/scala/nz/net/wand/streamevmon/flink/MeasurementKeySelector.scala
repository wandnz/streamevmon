package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.Measurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata._
import nz.net.wand.streamevmon.measurements.haberman._
import nz.net.wand.streamevmon.measurements.latencyts._

import org.apache.flink.api.java.functions.KeySelector

class MeasurementKeySelector[T <: Measurement] extends KeySelector[T, String] {
  override def getKey(value: T): String =
    value match {
      case m@(_: DNS | _: RichDNS) => s"DNS-${m.stream}"
      case m@(_: HTTP | _: RichHTTP) => s"HTTP-${m.stream}"
      case m@(_: ICMP | _: RichICMP) => s"ICMP-${m.stream}"
      case m@(_: TCPPing | _: RichTCPPing) => s"TCPPing-${m.stream}"
      case m@(_: Traceroute | _: RichTraceroute) => s"Traceroute-${m.stream}"
      case m@(_: LatencyTSAmpICMP) => s"LatencyTSAmpICMP-${m.stream}"
      case m@(_: LatencyTSSmokeping) => s"LatencyTSSmokeping-${m.stream}"
      case m@(_: Flow) => s"Flow-${m.stream}"
      case m@(_: Haberman) => s"Haberman-${m.stream}"
    }
}
