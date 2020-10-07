package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{InfluxContainerSpec, SeedData}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class InfluxHistoryConnectionTest extends InfluxContainerSpec {
  "InfluxDB container" should {
    "successfully ping" in {
      val influx =
        InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_)  => fail
      }, Duration.Inf)
    }
  }

  "InfluxHistoryConnection" should {
    before {
      Await.result(
        InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
          .database(container.database)
          .bulkWriteNative(
            Seq(
              SeedData.icmp.subscriptionLine,
              SeedData.dns.subscriptionLine,
              SeedData.traceroutePathlen.subscriptionLine,
              SeedData.http.subscriptionLine,
              SeedData.tcpping.subscriptionLine
            ) ++ SeedData.bigdata.flowsAsLineProtocol
          ),
        Duration.Inf
      )
    }

    "get ICMP data" in {
      getInfluxHistory.getIcmpData().head shouldBe SeedData.icmp.expected
    }

    "get DNS data" in {
      getInfluxHistory.getDnsData().head shouldBe SeedData.dns.expected
    }

    "get HTTP data" in {
      getInfluxHistory.getHttpData().head shouldBe SeedData.http.expected
    }

    "get TCPPing data" in {
      getInfluxHistory.getTcppingData().head shouldBe SeedData.tcpping.expected
    }

    "get Traceroute data" in {
      getInfluxHistory.getTracerouteData().head shouldBe SeedData.traceroutePathlen.expected
    }

    "get Flow data" in {
      getInfluxHistory.getFlowStatistics() shouldBe SeedData.bigdata.flowsExpected
    }

    "get data between a time range" in {
      val conn = getInfluxHistory
      conn.getIcmpData(end = SeedData.icmp.expected.time.minusSeconds(10)) shouldBe Seq()
      conn.getIcmpData(start = SeedData.icmp.expected.time.plusSeconds(10)) shouldBe Seq()
      conn
        .getIcmpData(
          start = SeedData.icmp.expected.time.minusSeconds(10),
          end = SeedData.icmp.expected.time.plusSeconds(10)
        )
        .head shouldBe SeedData.icmp.expected
    }

    "get all data" in {
      getInfluxHistory.getAllAmpData() shouldBe Seq(
        SeedData.icmp.expected,
        SeedData.dns.expected,
        SeedData.http.expected,
        SeedData.tcpping.expected,
        SeedData.traceroutePathlen.expected
      )
    }
  }
}
