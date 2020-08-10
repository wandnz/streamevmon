package nz.net.wand.streamevmon.measurements.latencyts

import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, RichMeasurement}

import java.time.Instant

import scala.math.round

/** Represents a latency measurement reported by fping, used with the
  * Smokeping latency monitoring software. Similar to an AMP RichICMP.
  *
  * @see [[nz.net.wand.streamevmon.measurements.amp.RichICMP RichICMP]]
  * @see [[nz.net.wand.streamevmon.flink.sources.LatencyTSSmokepingFileInputFormat LatencyTSSmokepingFileInputFormat]]
  * @see [[LatencyTSAmpICMP]]
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
case class LatencyTSSmokeping(
  stream: String,
  destination: String,
  family: String,
  time: Instant,
  median: Option[Double],
  loss: Int,
  results: Seq[Double]
) extends RichMeasurement with CsvOutputable with HasDefault {
  override def toString: String = {
    s"$destination," +
      s"${time.getEpochSecond.toInt}," + {
      if (median.isDefined) {
        f"${median.getOrElse(0.0)}%.3f,"
      }
      else {
        ","
      }
    } +
      f"${loss.toDouble}%.3f" +
      s"${
        if (results.isEmpty) {
          ""
        }
        else {
          ","
        }
      }" +
      results.map(i => f"$i%.3f").mkString(",")
  }

  override def isLossy: Boolean = loss > 0

  override def toCsvFormat: Seq[String] = LatencyTSSmokeping.unapply(this).get.productIterator.toSeq.map(toCsvEntry)

  var defaultValue: Option[Double] = median
}

object LatencyTSSmokeping {

  private def getFamily(in: String): String = {
    if (in.contains("v6")) {
      "ipv6"
    }
    else {
      "ipv4"
    }
  }

  private def getMedian(items: Seq[Double]): Option[Double] = {
    def roundTo3DP(n: Double): Double = {
      round(n * 1000).toDouble / 1000
    }

    if (items.isEmpty) {
      None
    }
    else {
      val s = items.sorted
      if (items.length % 2 != 0) {
        Some(roundTo3DP(s(items.length / 2)))
      }
      else {
        Some(roundTo3DP((s((items.length / 2) - 1) + s(items.length / 2)) / 2))
      }
    }
  }

  private def getLoss(in: Seq[Double]): Int = {
    20 - in.length
  }

  def create(line: String, streamId: String): LatencyTSSmokeping = {
    val fields = line.split(",", -1)

    val measurements = fields.drop(4).map(x => x.toDouble)

    LatencyTSSmokeping(
      streamId,
      fields(0),
      getFamily(fields(0)),
      Instant.ofEpochSecond(fields(1).toLong),
      getMedian(measurements),
      getLoss(measurements),
      measurements
    )
  }
}
