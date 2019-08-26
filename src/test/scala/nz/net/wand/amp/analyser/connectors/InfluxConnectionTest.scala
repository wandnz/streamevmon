package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.SeedData
import nz.net.wand.amp.analyser.measurements.MeasurementFactory

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.{Subscription, SubscriptionInfo}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class InfluxConnectionTest extends InfluxContainerSpec {

  def checkSubscription(influx: InfluxConnection, subscriptionInfo: SubscriptionInfo, checkPresent: Boolean): Unit = {
    checkSubscription(influx.influx.get, subscriptionInfo, checkPresent)
  }

  def checkSubscription(
      influx: AhcManagementClient,
      subscriptionInfo: SubscriptionInfo,
      checkPresent: Boolean
  ): Unit = {
    Await.result(
      influx.showSubscriptionsInfo.map {
        case Left(a) => fail(s"Checking subscription failed: $a")
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
    def getExpectedSubscriptionInfo(influx: InfluxConnection): SubscriptionInfo = {
      SubscriptionInfo(container.database,
                       Array(
                         Subscription(
                           container.retentionPolicy,
                           influx.subscriptionName,
                           Destinations.ALL,
                           influx.destinations
                         )))
    }

    "add a subscription" in {
      val influx = getInflux("addRemove")

      val expected = getExpectedSubscriptionInfo(influx)

      Await.result(influx.addSubscription(), Duration.Inf)

      checkSubscription(influx, expected, checkPresent = true)
    }

    "remove a subscription" in {
      val influx = getInflux("addRemove")
      val expected = getExpectedSubscriptionInfo(influx)

      checkSubscription(influx, expected, checkPresent = true)

      Await.result(influx.dropSubscription(), Duration.Inf)

      checkSubscription(influx, expected, checkPresent = false)
    }

    "clobber an existing subscription" in {
      val influx = getInflux("clobber")
      val expected = getExpectedSubscriptionInfo(influx)

      Await.result(influx.addSubscription(), Duration.Inf)

      checkSubscription(influx, expected, checkPresent = true)

      val newInflux = getInflux("clobber", "different-address")
      val newExpected = getExpectedSubscriptionInfo(newInflux)
      Await.result(newInflux.addOrUpdateSubscription(), Duration.Inf)

      checkSubscription(influx, expected, checkPresent = false)
      checkSubscription(newInflux, newExpected, checkPresent = true)

      Await.result(newInflux.dropSubscription(), Duration.Inf)
    }
  }

  var shouldSend = false
  private[this] lazy val ourClassLoader: ClassLoader = this.getClass.getClassLoader
  private[this] lazy val actorSystem =
    akka.actor.ActorSystem(getClass.getSimpleName, classLoader = Some(ourClassLoader))

  def sendData(andThen: () => Any = () => Unit): Unit = {
    val db = InfluxIO(container.address, container.port, Some(container.credentials))
      .database(container.database)

    actorSystem.scheduler
      .scheduleOnce(20 millis) {
        println("Sending data")
        Await.result(db.writeNative(SeedData.icmp.subscriptionLine), Duration.Inf)
        Thread.sleep(20)
        Await.result(db.writeNative(SeedData.dns.subscriptionLine), Duration.Inf)
        Thread.sleep(20)
        Await.result(db.writeNative(SeedData.traceroute.subscriptionLine), Duration.Inf)
        println("Data sent.")
        andThen()
      }
  }

  def sendDataAnd(
      afterSend: () => Any = () => Unit,
      withSend: () => Any = () => Unit
  ): Unit = {
    sendData(andThen = afterSend)
    withSend()
  }

  "Subscription listener" should {
    var isRunning: Boolean = false

    def getListener(influx: InfluxConnection): ServerSocket = {
      influx.getSubscriptionListener match {
        case Some(x) => x.setSoTimeout(100); x
        case None    => fail("No subscription listener obtained")
      }
    }

    "receive valid data" in {
      val influx = getInflux("receiveData")

      sendDataAnd(
        afterSend = { () =>
          println("Disabling flag")
          isRunning = false
        },
        withSend = { () =>
          val ssock = getListener(influx)
          println(s"Listening on ${ssock.getInetAddress}")
          var gotData = false

          isRunning = true
          while (isRunning) {
            try {
              val sock = ssock.accept
              sock.setSoTimeout(10)
              val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

              println("Receiving data")
              gotData = true

              val result = Stream
                .continually(reader.readLine)
                .takeWhile(line => line != null)
                .toList

              assert(result.nonEmpty)
              assert(result.exists(line => MeasurementFactory.createMeasurement(line).isDefined))

            } catch {
              case _: SocketTimeoutException =>
            }
          }

          assert(gotData)

          influx.stopSubscriptionListener(ssock)
        }
      )
    }
  }
}
