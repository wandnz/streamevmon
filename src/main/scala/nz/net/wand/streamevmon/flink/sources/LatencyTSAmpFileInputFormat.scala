package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import org.apache.flink.api.common.io.GenericCsvInputFormat

import scala.collection.mutable

/** An InputFormat which parses the AMP ICMP results from the Latency TS I
  * dataset.
  *
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSAmpFileInputFormat extends GenericCsvInputFormat[LatencyTSAmpICMP]
                                          with HasFlinkConfig {

  override val flinkName: String = "Latency TS Amp Source"
  override val flinkUid: String = "latency-ts-amp-source"
  override val configKeyGroup: String = "latencyts"

  override def openInputFormat(): Unit = {
    // We need parallelism 1 because we want to make unique stream IDs.
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this InputFormat must be 1.")
    }
  }

  val recordToStream: mutable.Map[String, Int] = mutable.Map()

  override def readRecord(
    reuse: LatencyTSAmpICMP,
    bytes: Array[Byte],
    offset: Int,
    numBytes: Int
  ): LatencyTSAmpICMP = {

    val line = new String(bytes.slice(offset, offset + numBytes))
    val key = line.split(",")(0)
    val stream = recordToStream.getOrElseUpdate(key, recordToStream.size)

    LatencyTSAmpICMP.create(line, stream)
  }
}
