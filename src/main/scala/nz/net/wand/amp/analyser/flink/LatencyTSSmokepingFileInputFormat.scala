package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.measurements.LatencyTSSmokeping

import org.apache.flink.api.common.io.GenericCsvInputFormat

/** An InputFormat which parses the Smokeping results from the Latency TS I
  * dataset.
  *
  * @see [[nz.net.wand.amp.analyser.measurements.LatencyTSSmokeping LatencyTSSmokeping]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSSmokepingFileInputFormat
  extends GenericCsvInputFormat[LatencyTSSmokeping] {

  override def readRecord(reuse: LatencyTSSmokeping,
                          bytes: Array[Byte],
                          offset: Int,
                          numBytes: Int): LatencyTSSmokeping = {
    val line = new String(bytes.slice(offset, offset + numBytes))

    LatencyTSSmokeping.create(line, 0)
  }
}
