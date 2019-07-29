package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.PostgresConnection

trait MeasurementFactory {

  val table_name: String

  protected def getNamedField(fields: Array[String], name: String): String = {
    fields
      .map(entry =>
        if (entry.startsWith(name)) {
          entry
        }
        else {
          ""
      })
      .filter(!_.isEmpty)
      .head
      .split('=')(1)
  }

  private[measurements] def create(subscriptionLine: String): Option[Measurement]
}

trait RichMeasurementFactory {
  private[measurements] def create(base: Measurement,
                                   meta: MeasurementMeta): Option[RichMeasurement]
}

object MeasurementFactory {

  def createMeasurement(line: String): Option[Measurement] = {
    line match {
      case x if x.startsWith(ICMP.table_name)       => ICMP.create(x)
      case x if x.startsWith(DNS.table_name)        => DNS.create(x)
      case x if x.startsWith(Traceroute.table_name) => Traceroute.create(x)
      case _                                        => None
    }
  }

  def enrichMeasurement(base: Measurement): Option[RichMeasurement] = {
    PostgresConnection.getMeta(base) match {
      case Some(x) =>
        x match {
          case y: ICMPMeta       => RichICMP.create(base, y)
          case y: DNSMeta        => RichDNS.create(base, y)
          case y: TracerouteMeta => RichTraceroute.create(base, y)
          case _                 => None
        }
      case None => None
    }
  }

  def createRichMeasurement(line: String): Option[RichMeasurement] = {
    createMeasurement(line).flatMap(_.enrich())
  }
}
