package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSSmokeping

import org.apache.flink.api.common.io.GenericCsvInputFormat

import scala.collection.mutable

/** An InputFormat which parses the Smokeping results from the Latency TS I
  * dataset.
  *
  * @see [[nz.net.wand.streamevmon.measurements.latencyts.LatencyTSSmokeping LatencyTSSmokeping]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSSmokepingFileInputFormat extends GenericCsvInputFormat[LatencyTSSmokeping]
                                                with HasFlinkConfig {

  override val flinkName: String = "Latency TS Smokeping Source"
  override val flinkUid: String = "latency-ts-smokeping-source"
  override val configKeyGroup: String = "latencyts.smokeping"

  override def openInputFormat(): Unit = {
    // We need parallelism 1 because we want to make unique stream IDs.
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this InputFormat must be 1.")
    }
  }

  val recordToStream: mutable.Map[String, Int] = mutable.Map()

  override def readRecord(
    reuse: LatencyTSSmokeping,
    bytes: Array[Byte],
    offset: Int,
    numBytes: Int
  ): LatencyTSSmokeping = {

    val line = new String(bytes.slice(offset, offset + numBytes))
    val key = line.split(",")(0)
    val stream = recordToStream.getOrElseUpdate(key, recordToStream.size)

    LatencyTSSmokeping.create(line, stream.toString)
  }
}
