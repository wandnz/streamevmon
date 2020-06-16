package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.connectors.PostgresConnection
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import org.squeryl.annotations.Column

import scala.annotation.tailrec
import scala.reflect.runtime.universe._
import scala.util.control.Breaks._

/** Mixed into companion objects of concrete [[Measurement]] classes.
  * Provides helper functions for common requirements to generate objects.
  *
  * @see [[RichInfluxMeasurementFactory]]
  * @see This trait's companion object for measurement creation functions.
  */
trait InfluxMeasurementFactory {

  /** The name of the InfluxDB table corresponding to a measurement type.
    */
  val table_name: String

  /** Returns the overrides declared via @Column annotations to case class field
    * names. The first item of each tuple is the field name, and the second
    * element is the value placed in the annotation.
    *
    * @tparam T The type to determine the overrides for.
    */
  private[this] def getColumnNameOverrides[T <: InfluxMeasurement : TypeTag]: Seq[(String, String)] =
    symbolOf[T].toType.members.map { m =>
      if (m.annotations.exists(a => a.tree.tpe <:< typeOf[Column])) {
        (
          m.name.toString.trim,
          m
            .annotations
            .find(a => a.tree.tpe <:< typeOf[Column])
            .get
            .tree.children.tail.head
            .collect { case Literal(Constant(value: String)) => value }
            .head
        )
      }
      else {
        Nil
      }
    }.filterNot(_ == Nil).asInstanceOf[Seq[(String, String)]]

  /** Returns a collection containing the database column names associated with
    * a type, in the same order as the case class declares them.
    */
  protected def getColumnNames[T <: InfluxMeasurement : TypeTag]: Seq[String] = {
    val overrides = getColumnNameOverrides[T]

    "time" +: typeOf[T].members.sorted.collect {
      case m: MethodSymbol if m.isCaseAccessor => m.name.toString
    }.filterNot(_ == "time").map { f =>
      val ov = overrides.find(_._1 == f)
      if (ov.isDefined) {
        ov.get._2
      }
      else {
        f
      }
    }
  }

  /** Returns a collection containing the database column names associated with
    * this type, in the same order as they are declared.
    */
  def columnNames: Seq[String]

  /** Searches a group of 'key=value' pairs for a particular key, and returns the
    * value.
    *
    * @param fields The group of 'key=value' pairs.
    * @param name   The key to find.
    *
    * @return The value if found, or None. If several are found, returns the first.
    */
  protected[this] def getNamedField(fields: Iterable[String], name: String): Option[String] = {
    fields
      .filter(entry => name == entry.split('=')(0))
      .map(entry => entry.split('=')(1))
      .headOption
  }

  /** Like string.split(), but it ignores separators that are inside double quotes.
    *
    * @param line              The line to split.
    * @param precedingElements The newly discovered elements are appended to this.
    * @param separators        The separators to split on.
    */
  @tailrec
  final protected[this] def splitLineProtocol(
    line: String,
    precedingElements: Seq[String] = Seq(),
    separators: Seq[Char] = Seq(',', ' ')
  ): Seq[String] = {
    var splitPoint = -1
    var quoteCount = 0
    // Iterate through the string, looking for separators.
    // Separators that are in quotes don't count, since they're part of a value.
    // We stop when we find the first one.
    breakable {
      for (i <- Range(0, line.length)) {
        if (line(i) == '"') {
          quoteCount += 1
        }
        if (quoteCount % 2 == 0 && separators.contains(line(i))) {
          splitPoint = i
          break
        }
      }
    }

    // If there aren't any left, we'll complete the seq and return it.
    if (splitPoint == -1) {
      precedingElements :+ line
    }
    // Otherwise, we'll split around the separator and give the rest to the
    // recursive function.
    else {
      val beforeSplit = line.substring(0, splitPoint)
      val afterSplit = line.substring(splitPoint + 1)
      splitLineProtocol(afterSplit, precedingElements :+ beforeSplit, separators)
    }
  }

  /** Creates a Measurement from an InfluxDB subscription result, in Line Protocol format.
    *
    * @param subscriptionLine The line received from the subscription.
    *
    * @return The Measurement object, or None if the creation failed.
    *
    * @see [[https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_reference/]]
    */
  private[measurements] def create(subscriptionLine: String): Option[InfluxMeasurement]

  /** Converts the "rtts" field, used in a number of AMP measurements, into an
    * appropriate datatype.
    *
    * @param in The value of the "rtts" field.
    *
    * @return A sequence of round-trip times.
    */
  protected[this] def getRtts(in: String): Seq[Option[Int]] = {
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
  * @see [[InfluxMeasurementFactory]]
  */
trait RichInfluxMeasurementFactory {

  /** Creates a RichMeasurement by mixing a Measurement with its associated
    * metadata.
    *
    * @param base The measurement.
    * @param meta The metadata associated with the measurement.
    *
    * @return The result if successful, or None.
    */
  private[measurements] def create(
    base                               : Measurement,
                                   meta: MeasurementMeta): Option[RichMeasurement]
}

/** Creates [[Measurement]] and [[RichMeasurement]] objects.
  *
  * @see [[InfluxMeasurementFactory]]
  * @see [[RichInfluxMeasurementFactory]]
  */
object InfluxMeasurementFactory {

  /** Creates a Measurement from a string in InfluxDB Line Protocol format.
    *
    * @param line The string describing the measurement.
    *
    * @return The measurement if successful, or None.
    */
  def createMeasurement(line: String): Option[InfluxMeasurement] = {
    line match {
      case x if x.startsWith(ICMP.table_name) => ICMP.create(x)
      case x if x.startsWith(DNS.table_name) => DNS.create(x)
      case x if x.startsWith(Traceroute.table_name) => Traceroute.create(x)
      case x if x.startsWith(TCPPing.table_name) => TCPPing.create(x)
      case x if x.startsWith(HTTP.table_name) => HTTP.create(x)
      case x if x.startsWith(Flow.table_name) => Flow.create(x)
      case _ => None
    }
  }

  /** Enriches a measurement.
    *
    * @param pgConnection A connection to a PostgreSQL server containing
    *                     metadata for the measurement.
    * @param base         The Measurement to enrich.
    *
    * @return The RichMeasurement if enrichment was successful, otherwise None.
    */
  def enrichMeasurement(
    pgConnection: PostgresConnection,
    base        : Measurement
  ): Option[RichInfluxMeasurement] = {
    pgConnection.getMeta(base) match {
      case Some(x) =>
        x match {
          case y: ICMPMeta => RichICMP.create(base, y)
          case y: DNSMeta => RichDNS.create(base, y)
          case y: TracerouteMeta => RichTraceroute.create(base, y)
          case y: TCPPingMeta => RichTCPPing.create(base, y)
          case y: HTTPMeta => RichHTTP.create(base, y)
          case _ => None
        }
      case None => None
    }
  }

  /** Creates a RichMeasurement directly from a string in InfluxDB Line Protocol format.
    *
    * @param pgConnection A connection to a PostgreSQL server containing
    *                     metadata for the measurement.
    * @param line         The string describing the measurement.
    *
    * @return The RichMeasurement if both measurement creation and enrichment
    *         were successful, otherwise None.
    */
  def createRichMeasurement(
    pgConnection: PostgresConnection,
    line        : String
  ): Option[RichInfluxMeasurement] = {
    createMeasurement(line).flatMap(enrichMeasurement(pgConnection, _))
  }
}
