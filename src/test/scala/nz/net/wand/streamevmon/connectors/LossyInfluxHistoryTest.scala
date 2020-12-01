package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.{InfluxContainerSpec, SeedData}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.scalatest.BeforeAndAfter

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class LossyInfluxHistoryTest extends InfluxContainerSpec with BeforeAndAfter {

  "InfluxIO" should {
    "write and read lossy data" when {
      before {
        Await
          .result(
            InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
              .database(container.database)
              .bulkWriteNative(
                Seq(
                  SeedData.icmp.subscriptionLine,
                  SeedData.icmp.lossySubscriptionLine,
                  SeedData.dns.subscriptionLine,
                  SeedData.dns.lossySubscriptionLine,
                  SeedData.tcpping.subscriptionLine,
                  SeedData.tcpping.lossySubscriptionLine,
                  SeedData.http.subscriptionLine,
                  SeedData.http.lossySubscriptionLine,
                )),
            Duration.Inf
          ) should not be a[Throwable]
      }

      "icmp" in {
        getInfluxHistory.getIcmpData().toList shouldBe List(SeedData.icmp.expected, SeedData.icmp.lossyExpected)
      }

      "dns" in {
        getInfluxHistory.getDnsData().toList shouldBe List(SeedData.dns.expected, SeedData.dns.lossyExpected)
      }

      "tcpping" in {
        getInfluxHistory.getTcppingData().toList shouldBe List(SeedData.tcpping.expected, SeedData.tcpping.lossyExpected)
      }

      "http" in {
        getInfluxHistory.getHttpData().toList shouldBe List(SeedData.http.expected, SeedData.http.lossyExpected)
      }
    }
  }
}
