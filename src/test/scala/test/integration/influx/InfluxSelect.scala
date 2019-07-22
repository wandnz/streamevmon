package test.integration.influx

import nz.net.wand.amp.analyser.InfluxConnection
import nz.net.wand.amp.analyser.measurements.{DNS, ICMP}

import java.net.ConnectException

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import com.github.fsanaulla.chronicler.macros.auto._
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future

class InfluxSelect extends AsyncFlatSpec {

  def selectICMP(): Future[ErrorOr[Array[ICMP]]] = {
    val influxDB = InfluxConnection
      .getIO("localhost", 8086, InfluxCredentials("cuz", ""))

    influxDB match {
      case Some(db) =>
        val database = "nntsc"
        val measurement = db.measurement[ICMP](database, ICMP.table_name)

        measurement.read(s"SELECT * FROM ${ICMP.table_name} fill(-1)")
      case None => Future(Left(new ConnectException("Connection to InfluxDB failed")))
    }
  }

  behavior of "selectICMP"

  it should "eventually obtain ICMP results from InfluxDB" in {
    val selectResult = selectICMP()

    selectResult.map(res => assert(res.isRight))
  }

  def selectDNS(): Future[ErrorOr[Array[DNS]]] = {
    val influxDB = InfluxConnection
      .getIO("localhost", 8086, InfluxCredentials("cuz", ""))

    influxDB match {
      case Some(db) =>
        val database = "nntsc"
        val measurement = db.measurement[DNS](database, DNS.table_name)

        measurement.read(s"SELECT * FROM ${DNS.table_name} fill(-1)")
      case None => Future(Left(new ConnectException("Connection to InfluxDB failed")))
    }
  }

  behavior of "selectDNS"

  it should "eventually obtain DNS results from InfluxDB" in {
    val selectResult = selectDNS()

    selectResult.map(res => assert(res.isRight))
  }
}
