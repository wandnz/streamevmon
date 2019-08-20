package nz.net.wand.amp.analyser.measurements

import java.time.Instant

/** Represents an AMP ICMP measurement, but only containing the data contained
  * in the Latency TS I dataset. Comparable to a RichICMP object, but missing
  * some fields.
  *
  * @see [[RichICMP]]
  * @see [[nz.net.wand.amp.analyser.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * @see [[LatencyTSSmokeping]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
case class LatencyTSAmpICMP(
    stream: Int,
    source: String,
    destination: String,
    family: String,
    time: Instant,
    average: Int,
    lossrate: Double
) extends RichMeasurement {
  override def toString: String = {
    f"$source-$destination-$family,${time.getEpochSecond.toInt},$average,$lossrate%.3f"
  }
}

object LatencyTSAmpICMP {
  val packet_size = 84

  def create(line: String, streamId: Int): LatencyTSAmpICMP = {
    val fields = line.split(',')
    val meta = fields(0).split('-')

    LatencyTSAmpICMP(
      streamId,
      meta(0),
      meta(1),
      meta(2),
      Instant.ofEpochSecond(fields(1).toLong),
      fields(2).toInt,
      fields(3).toDouble
    )
  }
}
