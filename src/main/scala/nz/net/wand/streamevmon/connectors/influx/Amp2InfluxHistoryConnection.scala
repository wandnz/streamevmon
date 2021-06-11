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
import nz.net.wand.streamevmon.measurements.amp2._

import java.time.Instant

import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxReader, ParsingException}
import com.github.fsanaulla.chronicler.urlhttp.io.UrlIOClient
import org.apache.flink.api.java.utils.ParameterTool

import scala.reflect._

/** InfluxDB connector which allows retrieving historical data. See the package
  * object for details on how to configure this class.
  */
case class Amp2InfluxHistoryConnection(
  dbName: String,
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
  def getAllAmp2Data(
    start: Instant = Instant.EPOCH,
    end  : Instant = Instant.now()
  ): Iterator[Amp2Measurement] = {
    Seq(
      (External.measurementName, Amp2Measurement.getColumnNames[External], InfluxSchema.amp2.external),
      (Fastping.measurementName, Amp2Measurement.getColumnNames[Fastping], InfluxSchema.amp2.fastping),
      (Http.measurementName, Amp2Measurement.getColumnNames[Http], InfluxSchema.amp2.http),
      (Latency.measurementName, Amp2Measurement.getColumnNames[Latency], InfluxSchema.amp2.latency),
      (Pathlen.measurementName, Amp2Measurement.getColumnNames[Pathlen], InfluxSchema.amp2.pathlen),
      (Sip.measurementName, Amp2Measurement.getColumnNames[Sip], InfluxSchema.amp2.sip),
      (Throughput.measurementName, Amp2Measurement.getColumnNames[Throughput], InfluxSchema.amp2.throughput),
      (Traceroute.measurementName, Amp2Measurement.getColumnNames[Traceroute], InfluxSchema.amp2.traceroute),
      (Udpstream.measurementName, Amp2Measurement.getColumnNames[Udpstream], InfluxSchema.amp2.udpstream),
      (Video.measurementName, Amp2Measurement.getColumnNames[Video], InfluxSchema.amp2.video),
    ).map { case (table, columns, reader) =>
      getData(table, columns, reader, start, end)
    }.fold(Iterator.empty)((a, b) => a ++ b)
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
  private def getData[T: ClassTag](
    tableName: String,
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

        logger.debug(s"Executing query on InfluxDB server $influxAddress:$influxPort: $query")

        measurement.readChunked(query, chunkSize = 1000)(reader).fold(
          throwable => throw throwable,
          value => value.flatMap {
            case Left(t) => t match {
              case pe: ParsingException =>
                logger.debug(s"Got ParsingException on table $tableName. The result was probably empty. Exception: $pe Query: $query")
                Iterator.empty
              case _ => throw t
            }
            case Right(it) => it
          }
        )

      case None => throw new IllegalStateException("No InfluxDB connection!")
    }
  }
}

/** Additional constructors for the companion class. */
object Amp2InfluxHistoryConnection extends Logging {
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
  def apply(p: ParameterTool, configPrefix: String = "influx", datatype: String = "amp2"): Amp2InfluxHistoryConnection =
    Amp2InfluxHistoryConnection(
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
