package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata._
import nz.net.wand.streamevmon.measurements.Measurement

import java.time.Instant

import com.github.fsanaulla.chronicler.ahc.io.{AhcIOClient, InfluxIO}
import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxReader}
import org.apache.flink.api.java.utils.ParameterTool

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.reflect._

/** Additional constructors for the companion class. */
object InfluxHistoryConnection {

  private[this] def getWithFallback(p: ParameterTool, configPrefix: String, datatype: String, item: String): String = {
    val result = p.get(s"$configPrefix.$datatype.$item", null)
    if (result == null) {
      p.get(s"$configPrefix.default.$item")
    }
    else {
      result
    }
  }

  /** Creates a new InfluxHistoryConnection from the given config. Expects all fields
    * specified in the companion class' main documentation to be present.
    *
    * @param p            The configuration to use. Generally obtained from the Flink
    *                     global configuration.
    * @param configPrefix A custom config prefix to use, in case the configuration
    *                     object is not as expected.
    *
    * @return A new InfluxHistoryConnection object.
    */
  def apply(p: ParameterTool, configPrefix: String = "influx.dataSource", datatype: String = "amp"): InfluxHistoryConnection =
    InfluxHistoryConnection(
      getWithFallback(p, configPrefix, datatype, "databaseName"),
      getWithFallback(p, configPrefix, datatype, "retentionPolicy"),
      getWithFallback(p, configPrefix, datatype, "serverName"),
      getWithFallback(p, configPrefix, datatype, "portNumber").toInt,
      getWithFallback(p, configPrefix, datatype, "user"),
      getWithFallback(p, configPrefix, datatype, "password")
    )
}

/** InfluxDB connector which allows retrieving historical data.
  *
  * Used in [[nz.net.wand.streamevmon.flink.InfluxSourceFunction InfluxSourceFunction]]
  * to catch up with data missed since the last checkpoint, and to get some history
  * at first startup.
  *
  * ==Configuration==
  *
  * This class is configured by the `influx.dataSource` config key group. See
  * [[InfluxConnection]] for additional details on how to configure this class.
  *
  * - `databaseName`: The name of the InfluxDB database to retrieve data from.
  * Default "nntsc".
  *
  * - `retentionPolicy`: The name of the retention policy to retrieve data from.
  * Default "nntscdefault".
  *
  * - `serverName`: The address that InfluxDB can be found at.
  * Default "localhost".
  *
  * - `portNumber`: The port that InfluxDB is listening on.
  * Default 8086.
  *
  * - `user`: The username that should be used to connect to InfluxDB.
  * Default "cuz"
  *
  * - `password`: The password that should be used to connect to InfluxDB.
  * Default "".
  *
  * @see [[nz.net.wand.streamevmon.flink.InfluxSinkFunction InfluxSinkFunction]]
  * @see [[PostgresConnection]]
  */
case class InfluxHistoryConnection(
    dbName: String,
    rpName: String,
    influxAddress: String,
    influxPort: Int,
    influxUsername: String,
    influxPassword: String
) extends Logging {

  private[connectors] lazy val influx: Option[AhcIOClient] = getClient

  private[this] def checkConnection(influx: AhcIOClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  /** @return The Influx IO connection, or None. */
  private[this] def getClient: Option[AhcIOClient] = {
    val influx: AhcIOClient = InfluxIO(influxAddress, influxPort, Some(InfluxCredentials(influxUsername, influxPassword)))

    Some(influx).filter(checkConnection)
  }

  /** Gets historical data for all supported measurement types over the specified
    * time range. All measurements returned should have a time between start and
    * end.
    *
    * @return A collection of Measurements of varying types, or an empty collection.
    */
  def getAllAmpData(
    start: Instant = Instant.EPOCH,
    end: Instant = Instant.now()
  ): Seq[Measurement] = {
    getIcmpData(start, end) ++
      getDnsData(start, end) ++
      getHttpData(start, end) ++
      getTcppingData(start, end) ++
      getTracerouteData(start, end)
  }

  /** This method does the heavy lifting of actually getting data from the DB.
    *
    * @param tableName   The table from which to get measurements - this should
    *                    be the table_name field attached to the object of type T.
    * @param columnNames The names of every column in the database for the chosen
    *                    table. This should be the columnNames field attached to
    *                    the object of type T.
    * @param reader      An InfluxReader object. See [[InfluxSchema]].
    * @param start       The oldest time to gather measurements from.
    * @param end         The newest time to gather measurements from.
    * @tparam T The type of Measurement the result is expected to be.
    *
    * @return
    */
  private def getData[T <: Measurement : ClassTag](
      tableName: String,
      columnNames: Seq[String],
      reader: InfluxReader[T],
      start: Instant,
      end: Instant
  ): Seq[T] = {
    influx match {
      case Some(db) =>
        val measurement = db.measurement[T](dbName, "")
        val query = s"SELECT ${columnNames.mkString("\"", "\",\"", "\"")} FROM $tableName " +
          s"WHERE time > ${start.toEpochMilli * 1000000} AND time <= ${end.toEpochMilli * 1000000}"
        val future = measurement.read(query)(reader, classTag[T])

        Await.result(future.flatMap {
          case Left(value)  => throw value
          case Right(value) => concurrent.Future(value)
        }, Duration.Inf)

      case None => throw new IllegalStateException("No InfluxDB connection!")
    }
  }

  /** Get some AMP ICMP measurements from InfluxDB.
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of ICMP measurements, or an empty collection.
    */
  def getIcmpData(
      start: Instant = Instant.EPOCH,
      end: Instant = Instant.now()
  ): Seq[ICMP] = {
    getData[ICMP](
      ICMP.table_name,
      ICMP.columnNames,
      InfluxSchema.icmpReader,
      start,
      end
    )
  }

  /** Get some AMP DNS measurements from InfluxDB.
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of DNS measurements, or an empty collection.
    */
  def getDnsData(
      start: Instant = Instant.EPOCH,
      end: Instant = Instant.now()
  ): Seq[DNS] = {
    getData[DNS](
      DNS.table_name,
      DNS.columnNames,
      InfluxSchema.dnsReader,
      start,
      end
    )
  }

  /** Get some AMP HTTP measurements from InfluxDB.
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of HTTP measurements, or an empty collection.
    */
  def getHttpData(
      start: Instant = Instant.EPOCH,
      end: Instant = Instant.now()
  ): Seq[HTTP] = {
    getData[HTTP](
      HTTP.table_name,
      HTTP.columnNames,
      InfluxSchema.httpReader,
      start,
      end
    )
  }

  /** Get some AMP TCPPing measurements from InfluxDB.
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of TCPPing measurements, or an empty collection.
    */
  def getTcppingData(
      start: Instant = Instant.EPOCH,
      end: Instant = Instant.now()
  ): Seq[TCPPing] = {
    getData[TCPPing](
      TCPPing.table_name,
      TCPPing.columnNames,
      InfluxSchema.tcppingReader,
      start,
      end
    )
  }

  /** Get some AMP Traceroute measurements from InfluxDB.
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of Traceroute measurements, or an empty collection.
    */
  def getTracerouteData(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now()
  ): Seq[Traceroute] = {
    getData[Traceroute](
      Traceroute.table_name,
      Traceroute.columnNames,
      InfluxSchema.tracerouteReader,
      start,
      end
    )
  }

  /** Get some libtrace-bigdata flow_statistics from InfluxDB
    *
    * @param start The oldest measurement should be no older than this.
    * @param end   The newest measurement should be no newer than this.
    *
    * @return A collection of Flow measurements, or an empty collection.
    */
  def getFlowStatistics(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now()
  ): Seq[Flow] = {
    getData[Flow](
      Flow.table_name,
      Flow.columnNames,
      InfluxSchema.flowStatisticsReader,
      start,
      end
    )
  }
}
