package nz.net.wand.amp.analyser.measurements

import nz.net.wand.amp.analyser.connectors.PostgresConnection

/** Mixed into companion objects of concrete [[Measurement]] classes.
  * Provides helper functions for common requirements to generate objects.
  *
  * @see [[RichMeasurementFactory]]
  * @see This trait's companion object for measurement creation functions.
  */
trait MeasurementFactory {

  /** The name of the InfluxDB table corresponding to a measurement type.
    */
  val table_name: String

  /** Searches a group of 'key=value' pairs for a particular key, and returns the
    * value.
    *
    * @param fields The group of 'key=value' pairs.
    * @param name   The key to find.
    * @return The value if found, or None. If several are found, returns the first.
    */
  protected[this] def getNamedField(fields: Iterable[String], name: String): Option[String] = {
    fields
      .filter(entry => entry.startsWith(name))
      .map(entry => entry.split('=')(1))
      .headOption
  }

  /** Creates a Measurement from an InfluxDB subscription result, in Line Protocol format.
    *
    * @param subscriptionLine The line received from the subscription.
    * @return The Measurement object, or None if the creation failed.
    * @see [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/]]
    */
  private[measurements] def create(subscriptionLine: String): Option[Measurement]

  /** Converts the "rtts" field, used in a number of AMP measurements, into an
    * appropriate datatype.
    *
    * @param in The value of the "rtts" field.
    * @return A sequence of round-trip times.
    */
  protected[this] def getRtts(in: String): Seq[Option[Int]] = {
    // TODO: Input assumed to be like "[1234, 3456]", including quotes
    in.drop(2).dropRight(2).split(',').map { x =>
      val y = x.trim
      if (y == "None") {
        None
      }
      else {
        Some(y.toInt)
      }
    }
  }
}

/** Mixed into companion objects of concrete [[RichMeasurement]] classes.
  *
  * @see [[MeasurementFactory]]
  */
trait RichMeasurementFactory {
  /** Creates a RichMeasurement by mixing a Measurement with its associated
    * metadata.
    *
    * @param base The measurement.
    * @param meta The metadata associated with the measurement.
    * @return The result if successful, or None.
    */
  private[measurements] def create(base: Measurement,
                                   meta: MeasurementMeta): Option[RichMeasurement]
}

/** Creates Measurements and RichMeasurements by passing calls through to
  * concrete implementations.
  *
  * @see [[MeasurementFactory]]
  * @see [[RichMeasurementFactory]]
  */
object MeasurementFactory {

  /** Creates a Measurement from a string in InfluxDB Line Protocol format.
    *
    * @param line The string describing the measurement.
    * @return The measurement if successful, or None.
    */
  def createMeasurement(line: String): Option[Measurement] = {
    line match {
      case x if x.startsWith(ICMP.table_name)       => ICMP.create(x)
      case x if x.startsWith(DNS.table_name)        => DNS.create(x)
      case x if x.startsWith(Traceroute.table_name) => Traceroute.create(x)
      case x if x.startsWith(TCPPing.table_name)    => TCPPing.create(x)
      case x if x.startsWith(HTTP.table_name)       => HTTP.create(x)
      case _                                        => None
    }
  }

  /** Enriches a measurement.
    *
    * @param base The Measurement to enrich.
    * @return The RichMeasurement if enrichment was successful, otherwise None.
    */
  def enrichMeasurement(base: Measurement): Option[RichMeasurement] = {
    PostgresConnection.getMeta(base) match {
      case Some(x) =>
        x match {
          case y: ICMPMeta       => RichICMP.create(base, y)
          case y: DNSMeta        => RichDNS.create(base, y)
          case y: TracerouteMeta => RichTraceroute.create(base, y)
          case y: TCPPingMeta    => RichTCPPing.create(base, y)
          case y: HTTPMeta       => RichHTTP.create(base, y)
          case _                 => None
        }
      case None => None
    }
  }

  /** Creates a RichMeasurement directly from a string in InfluxDB Line Protocol format.
    *
    * @param line The string describing the measurement.
    * @return The RichMeasurement if both measurement creation and enrichment
    *         were successful, otherwise None.
    */
  def createRichMeasurement(line: String): Option[RichMeasurement] = {
    createMeasurement(line).flatMap(_.enrich())
  }
}
