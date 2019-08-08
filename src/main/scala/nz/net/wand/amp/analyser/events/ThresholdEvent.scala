package nz.net.wand.amp.analyser.events

import java.time.Instant

import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint

import scala.collection.JavaConversions.mapAsJavaMap

case class ThresholdEvent(
    tags: Map[String, String] = Map(),
    severity: Int,
    time: Instant
) extends Event {

  implicit override def asInfluxPoint: InfluxDBPoint = {
    new InfluxDBPoint(
      ThresholdEvent.measurement_name,
      time.toEpochMilli,
      mapAsJavaMap(tags),
      mapAsJavaMap(
        Map[String, Object](
          "severity" -> new Integer(severity)
        ))
    )
  }
}

object ThresholdEvent {
  final val measurement_name = "threshold_events"
}
