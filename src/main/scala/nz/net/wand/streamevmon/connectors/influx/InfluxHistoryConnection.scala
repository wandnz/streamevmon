/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.connectors.influx

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata._
import nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement

import java.time.Instant

import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxReader, ParsingException}
import com.github.fsanaulla.chronicler.urlhttp.io.UrlIOClient
import org.apache.flink.api.java.utils.ParameterTool

import scala.reflect._

/** Additional constructors for the companion class. */
object InfluxHistoryConnection extends Logging {
  private def getWithFallback(p: ParameterTool, configPrefix: String, datatype: String, item: String): String = {
    val result = p.get(s"source.$configPrefix.$datatype.$item", null)
    if (result == null) {
      p.get(s"source.$configPrefix.$item")
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
  def apply(p: ParameterTool, configPrefix: String = "influx", datatype: String = "amp"): InfluxHistoryConnection =
    InfluxHistoryConnection(
      getWithFallback(p, configPrefix, datatype, "databaseName"),
      getWithFallback(p, configPrefix, datatype, "retentionPolicy"),
      getWithFallback(p, configPrefix, datatype, "historicalQueryProtocol"),
      getWithFallback(p, configPrefix, datatype, "compressHistoricalQueries").toBoolean,
      getWithFallback(p, configPrefix, datatype, "serverName"),
      getWithFallback(p, configPrefix, datatype, "portNumber").toInt,
      getWithFallback(p, configPrefix, datatype, "user"),
      getWithFallback(p, configPrefix, datatype, "password")
    )
}

/** InfluxDB connector which allows retrieving historical data. See the package
  * object for details on how to configure this class.
  */
case class InfluxHistoryConnection(
  dbName        : String,
  rpName        : String,
  protocol      : String,
  compress      : Boolean,
  influxAddress : String,
  influxPort    : Int,
  influxUsername: String,
  influxPassword: String
) extends Logging {

  private lazy val influx: Option[UrlIOClient] = getClient

  private def checkConnection(influx: UrlIOClient): Boolean = {
    influx.ping.isSuccess
  }

  /** @return The Influx IO connection, or None. */
  private def getClient: Option[UrlIOClient] = {
    val urlClient = new UrlIOClient(
      s"$protocol://$influxAddress",
      influxPort,
      Some(InfluxCredentials(influxUsername, influxPassword)),
      compress = compress
    )

    Some(urlClient).filter(checkConnection)
  }

  /** Gets historical data for all supported measurement types over the specified
    * time range. All measurements returned should have a time between start and
    * end.
    *
    * @return A collection of Measurements of varying types, or an empty collection.
    */
  def getAllAmpData(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now()
  ): Iterator[InfluxMeasurement] = {
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
  private def getData[T <: InfluxMeasurement : ClassTag](
    tableName  : String,
    columnNames: Seq[String],
    reader     : InfluxReader[T],
    start      : Instant,
    end        : Instant
  ): Iterator[T] = {
    influx match {
      case Some(db) =>
        val measurement = db.measurement[T](dbName, "")
        val query = s"SELECT ${columnNames.mkString("\"", "\",\"", "\"")} FROM $tableName " +
          s"WHERE time > ${start.toEpochMilli * 1000000} AND time <= ${end.toEpochMilli * 1000000}"

        measurement.readChunked(query, chunkSize = 1000)(reader).fold(
          throwable => throw throwable,
          value => value.flatMap {
            case Left(t) => t match {
              case pe: ParsingException =>
                logger.debug(s"Got ParsingException. The result was probably empty. $pe")
                Iterator.empty
              case _ => throw t
            }
            case Right(it) => it
          }
        )

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
    end  : Instant = Instant.now()
  ): Iterator[ICMP] = {
    getData[ICMP](
      ICMP.table_name,
      ICMP.columnNames,
      InfluxSchema.amp.icmpReader,
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
    end  : Instant = Instant.now()
  ): Iterator[DNS] = {
    getData[DNS](
      DNS.table_name,
      DNS.columnNames,
      InfluxSchema.amp.dnsReader,
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
    end  : Instant = Instant.now()
  ): Iterator[HTTP] = {
    getData[HTTP](
      HTTP.table_name,
      HTTP.columnNames,
      InfluxSchema.amp.httpReader,
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
    end  : Instant = Instant.now()
  ): Iterator[TCPPing] = {
    getData[TCPPing](
      TCPPing.table_name,
      TCPPing.columnNames,
      InfluxSchema.amp.tcppingReader,
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
  ): Iterator[TraceroutePathlen] = {
    getData[TraceroutePathlen](
      TraceroutePathlen.table_name,
      TraceroutePathlen.columnNames,
      InfluxSchema.amp.traceroutePathlenReader,
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
  ): Iterator[Flow] = {
    getData[Flow](
      Flow.table_name,
      Flow.columnNames,
      InfluxSchema.bigdata.flowStatisticsReader,
      start,
      end
    )
  }
}
