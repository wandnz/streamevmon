package nz.net.wand.amp.analyser

import java.net.ConnectException

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.{InfluxCredentials, InfluxDBInfo}
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future

class InfluxPing extends AsyncFlatSpec {

  def pingInflux(): Future[ErrorOr[InfluxDBInfo]] = {
    val influxDB = InfluxConnection.getManagement(
      "localhost",
      8086,
      InfluxCredentials("cuz", "")
    )

    influxDB match {
      case Some(db) => db.ping
      case None     => Future(Left(new ConnectException("Connection to InfluxDB failed")))
    }
  }

  behavior of "pingInflux"

  it should "eventually obtain a response from InfluxDB" in {
    val pingResult = pingInflux()

    pingResult.map(res => assert(res.isRight))
  }
}
