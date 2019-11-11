package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.SeedData

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import org.scalatest.BeforeAndAfter

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class InfluxHistoryConnectionTest extends InfluxContainerSpec with BeforeAndAfter {
  "InfluxDB container" should {
    "successfully ping" in {
      val influx =
        InfluxIO(container.address, container.port, Some(container.credentials))

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_)  => fail
      }, Duration.Inf)
    }
  }

  private def fillInflux(): Unit = {
    val db = InfluxIO(container.address, container.port, Some(container.credentials))
      .database(container.database)
    Await.result(
      db.bulkWriteNative(
        Seq(
          SeedData.icmp.subscriptionLine,
          SeedData.dns.subscriptionLine,
          SeedData.traceroute.subscriptionLine,
          SeedData.http.subscriptionLine,
          SeedData.tcpping.subscriptionLine
        )),
      Duration.Inf
    )
  }

  "InfluxHistoryConnection" should {

    before {
      fillInflux()
    }

    "get ICMP data" in {
      assert(SeedData.icmp.expected === getInfluxHistory.getIcmpData().head)
    }

    "get DNS data" in {
      assert(SeedData.dns.expected === getInfluxHistory.getDnsData().head)
    }

    "get HTTP data" in {
      assert(SeedData.http.expected === getInfluxHistory.getHttpData().head)
    }

    "get TCPPing data" in {
      assert(SeedData.tcpping.expected === getInfluxHistory.getTcppingData().head)
    }

    "get Traceroute data" in {
      assert(SeedData.traceroute.expected === getInfluxHistory.getTracerouteData().head)
    }

    "get data between a time range" in {
      val conn = getInfluxHistory
      assert(Seq() === conn.getIcmpData(end = SeedData.icmp.expected.time.minusSeconds(10)))
      assert(Seq() === conn.getIcmpData(start = SeedData.icmp.expected.time.plusSeconds(10)))
      assert(
        SeedData.icmp.expected === conn
          .getIcmpData(
            start = SeedData.icmp.expected.time.minusSeconds(10),
            end = SeedData.icmp.expected.time.plusSeconds(10)
          )
          .head)
    }

    "get all data" in {
      assert(Seq(
        SeedData.icmp.expected,
        SeedData.dns.expected,
        SeedData.http.expected,
        SeedData.tcpping.expected,
        SeedData.traceroute.expected
      ) === getInfluxHistory.getAllData())
    }
  }
}
