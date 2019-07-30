package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements.{DNS, ICMP}

import java.net.ConnectException
import java.time.Instant

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxReader}
import jawn.ast.JArray
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future

class InfluxSelect extends AsyncFlatSpec {

  val cols = Array(
    "time",
    "stream",
    "flag_aa",
    "flag_ad",
    "flag_cd",
    "flag_qr",
    "flag_ra",
    "flag_rd",
    "flag_tc",
    "lossrate",
    "opcode",
    "query_len",
    "rcode",
    "requests",
    "response_size",
    "rtt",
    "total_additional",
    "total_answer",
    "total_authority",
    "ttl"
  )

  behavior of "selectICMP"

  it should "eventually obtain ICMP results from InfluxDB" in {
    val selectResult = selectICMP()

    selectResult.map(res => assert(res.isRight))
  }
  implicit val rd: InfluxReader[DNS] = new InfluxReader[DNS] {
    override def read(js: JArray): ErrorOr[DNS] = {
      Right(
        DNS(
          js.get(cols.indexOf("stream")).asInt,
          js.get(cols.indexOf("flag_aa")).getBoolean,
          js.get(cols.indexOf("flag_ad")).getBoolean,
          js.get(cols.indexOf("flag_cd")).getBoolean,
          js.get(cols.indexOf("flag_qr")).getBoolean,
          js.get(cols.indexOf("flag_ra")).getBoolean,
          js.get(cols.indexOf("flag_rd")).getBoolean,
          js.get(cols.indexOf("flag_tc")).getBoolean,
          js.get(cols.indexOf("lossrate")).asDouble,
          js.get(cols.indexOf("opcode")).getInt,
          js.get(cols.indexOf("query_len")).asInt,
          js.get(cols.indexOf("rcode")).getInt,
          js.get(cols.indexOf("requests")).asInt,
          js.get(cols.indexOf("response_size")).getInt,
          js.get(cols.indexOf("rtt")).getInt,
          js.get(cols.indexOf("total_additional")).getInt,
          js.get(cols.indexOf("total_answer")).getInt,
          js.get(cols.indexOf("total_authority")).getInt,
          js.get(cols.indexOf("ttl")).getInt,
          Instant.parse(js.get(cols.indexOf("time")).asString)
        ))
    }
  }

  def selectICMP(): Future[ErrorOr[Array[ICMP]]] = {
    val influxDB = InfluxConnection
      .getIO("localhost", 8086, InfluxCredentials("cuz", ""))

    influxDB match {
      case Some(db) =>
        val database = "nntsc"
        val measurement = db.measurement[ICMP](database, ICMP.table_name)

        Future(Right(Array())) // TODO
      //measurement.read(s"SELECT * FROM ${ICMP.table_name} fill(-1)")
      case None => Future(Left(new ConnectException("Connection to InfluxDB failed")))
    }
  }

  def selectDNS(): Future[ErrorOr[Array[DNS]]] = {
    val influxDB = InfluxConnection
      .getIO("localhost", 8086, InfluxCredentials("cuz", ""))

    influxDB match {
      case Some(db) =>
        val columns = cols.mkString(",")
        println(columns)

        val m = db.measurement[DNS]("nntsc", DNS.table_name)
        m.read(s"SELECT $columns FROM ${DNS.table_name}")

      /*val database = "nntsc"
        val measurement = db.measurement[DNS](database, DNS.table_name)

        measurement.read(s"SELECT * FROM ${DNS.table_name} fill(-1)")

       */
      case None => Future(Left(new ConnectException("Connection to InfluxDB failed")))
    }
  }

  behavior of "selectDNS"

  it should "eventually obtain DNS results from InfluxDB" in {
    val selectResult = selectDNS()
    //selectResult.foreach(_.right.get.foreach(println))

    selectResult.map(res => assert(res.isRight))
  }
}
