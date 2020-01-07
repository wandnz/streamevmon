package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import org.apache.flink.api.common.io.GenericCsvInputFormat

import scala.collection.mutable

/** An InputFormat which parses the AMP ICMP results from the Latency TS I
  * dataset.
  *
  * @see [[nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP LatencyTSAmpICMP]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSAmpFileInputFormat extends GenericCsvInputFormat[LatencyTSAmpICMP] {

  override def openInputFormat(): Unit = {
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this InputFormat must be 1.")
    }
  }

  val recordToStream: mutable.Map[String, Int] = mutable.Map()

  override def readRecord(reuse: LatencyTSAmpICMP,
                          bytes: Array[Byte],
                          offset: Int,
                          numBytes: Int): LatencyTSAmpICMP = {

    val line = new String(bytes.slice(offset, offset + numBytes))
    val key = line.split(",")(0)
    val stream = recordToStream.getOrElseUpdate(key, recordToStream.size)

    LatencyTSAmpICMP.create(line, stream)
  }
}
