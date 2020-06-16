package nz.net.wand.streamevmon.measurements.latencyts

import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, RichMeasurement}

import java.time.Instant

/** Represents an AMP ICMP measurement, but only containing the data contained
  * in the Latency TS I dataset. Comparable to a RichICMP object, but missing
  * some fields.
  *
  * @see [[nz.net.wand.streamevmon.measurements.amp.RichICMP RichICMP]]
  * @see [[nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * @see [[LatencyTSSmokeping]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
case class LatencyTSAmpICMP(
  stream  : Int,
  source  : String,
  destination: String,
  family: String,
  time: Instant,
  average: Int,
  lossrate: Double
) extends RichMeasurement with CsvOutputable with HasDefault {
  override def toString: String = {
    f"$source-$destination-$family,${time.getEpochSecond.toInt},$average,$lossrate%.3f"
  }

  override def isLossy: Boolean = lossrate > 0.0

  override def toCsvFormat: Seq[String] = LatencyTSAmpICMP.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = Some(average)
}

object LatencyTSAmpICMP {
  val packet_size = 84

  def create(line: String, streamId: Int): LatencyTSAmpICMP = {
    val fields = line.split(',')
    val meta = fields(0).split('-')

    // TODO: Make sure the isLossy field is set up correctly here.

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
