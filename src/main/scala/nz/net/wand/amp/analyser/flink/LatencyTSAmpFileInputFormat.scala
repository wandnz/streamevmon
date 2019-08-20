package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.measurements.LatencyTSAmpICMP
import nz.net.wand.amp.analyser.Logging

import org.apache.flink.api.common.io.GenericCsvInputFormat

/** An InputFormat which parses the AMP ICMP results from the Latency TS I
  * dataset.
  *
  * @see [[nz.net.wand.amp.analyser.measurements.LatencyTSAmpICMP LatencyTSAmpICMP]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSAmpFileInputFormat extends GenericCsvInputFormat[LatencyTSAmpICMP] with Logging {

  override def readRecord(reuse: LatencyTSAmpICMP,
                          bytes: Array[Byte],
                          offset: Int,
                          numBytes: Int): LatencyTSAmpICMP = {
    val line = new String(bytes.slice(offset, offset + numBytes))

    LatencyTSAmpICMP.create(line, 0)
  }
}
