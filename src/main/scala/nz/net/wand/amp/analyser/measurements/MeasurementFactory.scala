package nz.net.wand.amp.analyser.measurements

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

object MeasurementFactory {

  def createMeasurement(line: String): Option[Measurement] = {
    line match {
      case x if x.startsWith(ICMP.table_name)       => ICMP.create(x)
      case x if x.startsWith(DNS.table_name)        => DNS.create(x)
      case x if x.startsWith(Traceroute.table_name) => Traceroute.create(x)
      case _                                        => None
    }
  }
}
