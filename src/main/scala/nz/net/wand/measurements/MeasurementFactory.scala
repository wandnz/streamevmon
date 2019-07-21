package nz.net.wand.measurements

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

  private[measurements] def Create(subscriptionLine: String): Option[Measurement]
}

object MeasurementFactory {

  def CreateMeasurement(line: String): Option[Measurement] = {
    line match {
      case x if x.startsWith(ICMP.table_name) => ICMP.Create(x)
      case x if x.startsWith(DNS.table_name)  => DNS.Create(x)
      case _                                  => None
    }
  }
}
