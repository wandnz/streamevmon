package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.{InfluxContainerSpec, SeedData}
import nz.net.wand.amp.analyser.flink.{MeasurementSourceFunction, MockSourceContext}
import nz.net.wand.amp.analyser.measurements.{Measurement, MeasurementFactory}

import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, SocketTimeoutException}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.{Subscription, SubscriptionInfo}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class InfluxConnectionTest extends InfluxContainerSpec {

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

    def getExpectedSubscriptionInfo: SubscriptionInfo = {
      SubscriptionInfo(InfluxConnection.dbName,
                       Array(
                         Subscription(
                           InfluxConnection.rpName,
                           InfluxConnection.subscriptionName,
                           Destinations.ALL,
                           InfluxConnection.destinations
                         )))
    }

    "add a subscription" in {

      InfluxConnection.subscriptionName = "addRemove"

      val expected = getExpectedSubscriptionInfo

      Await.result(InfluxConnection.addSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)
    }

    "remove a subscription" in {

      val expected = getExpectedSubscriptionInfo

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)

      Await.result(InfluxConnection.dropSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = false)
    }

    "clobber an existing subscription" in {

      InfluxConnection.subscriptionName = "clobber"

      val expected = getExpectedSubscriptionInfo

      Await.result(InfluxConnection.addSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = true)

      val oldAddress = InfluxConnection.listenAddress

      // This forces the subscription to be different from the existing one
      InfluxConnection.listenAddress = "fakeAddress.local"
      val newExpected = getExpectedSubscriptionInfo
      Await.result(InfluxConnection.addOrUpdateSubscription(), Duration.Inf)

      checkSubscription(InfluxConnection.influx.get, expected, checkPresent = false)
      checkSubscription(InfluxConnection.influx.get, newExpected, checkPresent = true)

      Await.result(InfluxConnection.dropSubscription(), Duration.Inf)

      InfluxConnection.listenAddress = oldAddress
    }
  }

  var shouldSend = false

  def sendData(): Unit = {
    val db = InfluxIO(container.address, container.port, Some(container.credentials))
      .database(container.database)

    Thread.sleep(20)

    println("Sending data")
    Await.result(db.writeNative(SeedData.icmp.subscriptionLine), Duration.Inf)
    Thread.sleep(20)
    Await.result(db.writeNative(SeedData.dns.subscriptionLine), Duration.Inf)
    Thread.sleep(20)
    Await.result(db.writeNative(SeedData.traceroute.subscriptionLine), Duration.Inf)
    println("Data sent.")
    Thread.sleep(20)
  }

  def sendDataAnd(
      afterSend: () => Any = () => Unit,
      withSend: () => Any = () => Unit
  ): Unit = {
    val withSendFuture = Future(withSend())
    val sendDataFuture = Future(sendData())

    Await.result(sendDataFuture, Duration.Inf)
    afterSend()
    Await.result(withSendFuture, Duration.Inf)
  }

  "Subscription listener" should {
    var isRunning: Boolean = false

    def getListener: ServerSocket = {
      InfluxConnection.getSubscriptionListener match {
        case Some(x) => x.setSoTimeout(100); x
        case None    => fail("No subscription listener obtained")
      }
    }

    "receive valid data" in {
      InfluxConnection.subscriptionName = "receiveData"

      sendDataAnd(
        afterSend = { () =>
          println("Disabling flag")
          isRunning = false
        },
        withSend = { () =>
          val ssock = getListener
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

          InfluxConnection.stopSubscriptionListener(ssock)
        }
      )
    }
  }

  "MeasurementSourceFunction" should {
    "receive valid data" in {
      InfluxConnection.subscriptionName = "mockMeasurementSourceContext"

      val func = new MeasurementSourceFunction
      val ctx = new MockSourceContext[Measurement] {
        override var process: Seq[Measurement] => Unit = { elements =>
          assert(elements.nonEmpty)
        }
      }

      sendDataAnd(afterSend = { () =>
        func.cancel()
        ctx.close()
      }, withSend = () => func.run(ctx))
    }
  }
}
