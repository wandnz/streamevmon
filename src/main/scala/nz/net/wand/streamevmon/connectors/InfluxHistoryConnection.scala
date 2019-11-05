package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements._

import java.time.Instant

import com.github.fsanaulla.chronicler.ahc.io.{AhcIOClient, InfluxIO}
import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxReader}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.reflect._

case class InfluxHistoryConnection(
    dbName: String,
    rpName: String,
    influxAddress: String,
    influxPort: Int,
    influxUsername: String,
    influxPassword: String
) extends Logging {

  private[connectors] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[connectors] lazy val influx: Option[AhcIOClient] = getClient

  private[this] def checkConnection(influx: AhcIOClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  private[this] def getClient: Option[AhcIOClient] = {
    def influx = InfluxIO(influxAddress, influxPort, Some(influxCredentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }

  private def getData[T: ClassTag](
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
          s"WHERE time > ${start.toEpochMilli * 1000000} AND time <= ${end.toEpochMilli * 1000000} " +
          s"fill(-1)"
        val future = measurement.read(query)(reader, classTag[T])

        Await.result(future.flatMap {
          case Left(value)  => throw value
          case Right(value) => concurrent.Future(value)
        }, Duration.Inf)

      case None => throw new IllegalStateException("No InfluxDB connection!")
    }
  }

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

  def getTracerouteData(
      start: Instant = Instant.EPOCH,
      end: Instant = Instant.now()
  ): Seq[Traceroute] = {
    getData[Traceroute](
      Traceroute.table_name,
      Traceroute.columnNames,
      InfluxSchema.tracerouteReader,
      start,
      end
    )
  }
}
