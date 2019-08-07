package nz.net.wand.amp.analyser.events

import java.time.Instant

import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint

abstract class Event {
  val severity: Int
  val time: Instant

  def asInfluxPoint: InfluxDBPoint
}
