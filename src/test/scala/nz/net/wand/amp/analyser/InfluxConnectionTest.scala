package nz.net.wand.amp.analyser

import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.{Subscription, SubscriptionInfo}
import org.scalatest.WordSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class InfluxConnectionTest extends WordSpec with ForAllTestContainer {
  override val container: InfluxDBContainer = InfluxDBContainer("alpine")

  override def afterStart(): Unit = {
    val influx =
      InfluxMng(container.address, container.port, Some(container.credentials))

    Await.result(influx.createRetentionPolicy(
                   container.retentionPolicy,
                   container.database,
                   "8760h0m0s",
                   default = true
                 ),
                 Duration.Inf)

    InfluxConnection.influx = Some(influx)
    InfluxConnection.dbName = container.database
    InfluxConnection.rpName = container.retentionPolicy
  }

  def checkSubscription(
      influx: AhcManagementClient,
      subscriptionInfo: SubscriptionInfo,
      checkPresent: Boolean
  ): Unit = {
    Await.result(
      influx.showSubscriptionsInfo.map {
        case Left(a) => fail(s"Adding subscription failed: $a")
        case Right(b) =>
          if (checkPresent) {
            val rightDb = b.filter(c => subscriptionInfo.dbName == c.dbName)
            assert(rightDb.length >= 0)
            rightDb.foreach { c =>
              assert(c.subscriptions.contains(subscriptionInfo.subscriptions.head))
            }
          }
          else {
            val rightDb = b.filter(c => subscriptionInfo.dbName == c.dbName)
            if (rightDb.length == 0) {
              succeed
            }
            else {
              rightDb.foreach { c =>
                assert(!c.subscriptions.contains(subscriptionInfo.subscriptions.head))
              }
            }
          }
      },
      Duration.Inf
    )
  }

  "InfluxDB container" should {

    "successfully ping" in {

      val influx =
        InfluxMng(container.address, container.port, Some(container.credentials))

      Await.result(influx.ping.map {
        case Right(_) => succeed
        case Left(_)  => fail
      }, Duration.Inf)
    }

    "authenticate, by adding and removing a subscription" in {
      val influx =
        InfluxMng(container.address, container.port, Some(container.credentials))

      val subscriptionName = "basicAuthAddRemove"
      val destinations = Seq("http://localhost:3456")

      val expected = SubscriptionInfo(container.database,
                                      Array(
                                        Subscription(
                                          container.retentionPolicy,
                                          subscriptionName,
                                          Destinations.ALL,
                                          destinations
                                        )))

      Await.result(
        influx.createSubscription(
          subscriptionName,
          container.database,
          container.retentionPolicy,
          Destinations.ALL,
          destinations
        ),
        Duration.Inf
      )

      checkSubscription(influx, expected, checkPresent = true)

      Await.result(
        influx.dropSubscription(
          subscriptionName,
          container.database,
          container.retentionPolicy
        ),
        Duration.Inf
      )

      checkSubscription(influx, expected, checkPresent = false)
    }
  }

  "InfluxConnection" should {

    def getConnectorSubscriptionInfo: SubscriptionInfo = {
      SubscriptionInfo(InfluxConnection.dbName,
                       Array(
                         Subscription(
                           InfluxConnection.rpName,
                           InfluxConnection.subscriptionName,
                           Destinations.ALL,
                           InfluxConnection.destinations
                         )))
    }

    "successfully ping" in {
      assert(InfluxConnection.checkConnection())
    }

    "add a subscription" in {

      InfluxConnection.subscriptionName = "addRemove"

      val expected = getConnectorSubscriptionInfo

      Await.result(InfluxConnection.addSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)
    }

    "remove a subscription" in {

      val expected = getConnectorSubscriptionInfo

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)

      Await.result(InfluxConnection.dropSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = false)
    }

    "clobber an existing subscription" in {
      InfluxConnection.subscriptionName = "clobber"

      val expected = getConnectorSubscriptionInfo

      Await.result(InfluxConnection.addSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)

      // This forces the subscription to be different from the existing one
      InfluxConnection.listenAddress = "fakeAddress.local"
      val newExpected = getConnectorSubscriptionInfo
      Await.result(InfluxConnection.addOrUpdateSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = false)
      checkSubscription(InfluxConnection.influx.get, newExpected, checkPresent = true)
    }
  }
}
