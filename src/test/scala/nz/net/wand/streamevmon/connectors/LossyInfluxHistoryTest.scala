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
                  SeedData.dns.lossySubscriptionLine
                )),
            Duration.Inf
          ) should not be a[Throwable]
      }

      "icmp" in {
        getInfluxHistory.getIcmpData().toSeq shouldBe Seq(SeedData.icmp.expected, SeedData.icmp.lossyExpected)
      }

      "dns" in {
        getInfluxHistory.getDnsData().toSeq shouldBe Seq(SeedData.dns.expected, SeedData.dns.lossyExpected)
      }

      "tcpping" ignore {
        // We don't have any examples of lossy TCPPing measurements, but we're
        // fairly sure they exist. It would be silly to test our tcppingReader
        // for lossy measurements when we are only guessing.
      }
    }
  }
}
