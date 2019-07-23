package nz.net.wand.amp.analyser.measurements

import com.github.fsanaulla.chronicler.macros.annotations.reader.utc
import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

final case class Traceroute(
    @tag stream: String,
    @field path_length: Double,
    @utc @timestamp time: Long
) extends Measurement {
  override def toString: String = {
    s"${Traceroute.table_name}," +
      s"stream=$stream " +
      s"path_length=$path_length " +
      s"$time"
  }

  override def enrich(): Option[RichTraceroute] = {
    MeasurementFactory.enrichMeasurement(this).asInstanceOf[Option[RichTraceroute]]
  }
}

object Traceroute extends MeasurementFactory {

  final override val table_name: String = "data_amp_traceroute_pathlen"

  override def create(subscriptionLine: String): Option[Traceroute] = {
    val data = subscriptionLine.split(Array(',', ' '))
    val namedData = data.drop(1).dropRight(1)
    if (data(0) != table_name) {
      None
    }
    else {
      Some(
        Traceroute(
          getNamedField(namedData, "stream"),
          getNamedField(namedData, "path_length").toDouble,
          data.last.toLong
        ))
    }
  }
}
