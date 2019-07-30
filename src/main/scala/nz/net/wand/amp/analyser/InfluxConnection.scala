package nz.net.wand.amp.analyser

import java.io.IOException
import java.net.{ConnectException, InetAddress, ServerSocket}

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread

object InfluxConnection extends Logging with Configuration {

  configPrefix = "influx.dataSource"
  private[this] val subscriptionName: String =
    getConfigString("subscriptionName").getOrElse("SubscriptionServer")
  private[this] val dbName: String = getConfigString("databaseName").getOrElse("nntsc")
  private[this] val rpName: String =
    getConfigString("retentionPolicyName").getOrElse("nntscdefault")
  private[this] val listenProtocol: String = getConfigString("listenProtocol").getOrElse("http")
  private[this] val listenAddress: String = getConfigString("listenAddress").get
  private[this] val listenPort: Int = getConfigInt("listenPort").getOrElse(8008)
  private[this] val listenBacklog: Int = getConfigInt("listenBacklog").getOrElse(5)
  private[this] val influxAddress: String = getConfigString("serverName").getOrElse("localhost")
  private[this] val influxPort: Int = getConfigInt("portNumber").getOrElse(8086)
  private[this] val influxUsername: String = getConfigString("user").getOrElse("cuz")
  private[this] val influxPassword: String = getConfigString("password").getOrElse("")
  var influx: Option[AhcManagementClient] = None
  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  def ensureConnection(): Unit = {
    influx match {
      case Some(_) =>
      case None    => influx = getManagement
    }
  }

  def disconnect(): Unit = {
    influx.foreach(_.close)
  }

  def getSubscriptionListener: Option[ServerSocket] = {
    try {
      val ssock = new ServerSocket(listenPort, listenBacklog, listenInet)
      ssock.setSoTimeout(100)
      val addOrUpdateResult = addOrUpdateSubscription().map {
        case Right(_) => Some(ssock)
        case Left(ex) =>
          logger.error("Error starting listener: " + ex)
          None
      }
      Await.result(addOrUpdateResult, Duration.Inf)
    } catch {
      case ex @ (_: ConnectException | _: SecurityException | _: IOException |
          _: IllegalArgumentException) =>
        logger.error("Error starting listener: " + ex)
        None
    }
  }

  def stopSubscriptionListener(ssock: ServerSocket): Unit = {
    Await.result(dropSubscription(), Duration.Inf)
    ssock.close()
  }

  def addSubscription(): Future[ErrorOr[ResponseCode]] = {
    ensureConnection()

    influx match {
      case Some(db) =>
        db.createSubscription(
            subscriptionName,
            dbName,
            rpName,
            Destinations.ALL,
            destinations
          )
          .flatMap { subscribeResult =>
            if (subscribeResult.isRight) {
              subscriptionRemoveHooks = subscriptionRemoveHooks :+
                (subscriptionName,
                sys.addShutdownHook {
                  Await.result(dropSubscription(), Duration.Inf)
                  logger.info(s"Removed subscription $subscriptionName")
                })
              logger.info(s"Added subscription $subscriptionName")
              Future(Right(subscribeResult.right.get))
            }
            else {
              Future(Left(subscribeResult.left.get))
            }
          }
      case None => Future(Left(new IllegalStateException("No influx connection.")))
    }
  }

  def dropSubscription(): Future[ErrorOr[ResponseCode]] = {
    ensureConnection()

    influx match {
      case Some(db) =>
        subscriptionRemoveHooks.filter(_._1 == subscriptionName).foreach { hook =>
          if (!hook._2.isAlive) {
            hook._2.remove
          }
        }
        db.dropSubscription(subscriptionName, dbName, rpName)
      case None => Future(Left(new IllegalStateException("No influx connection.")))
    }
  }

  def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] = {
    addSubscription().flatMap(addResult =>
      if (addResult.isRight) {
        Future(addResult)
      }
      else {
        dropSubscription().flatMap(dropResult =>
          if (dropResult.isRight) {
            addSubscription()
          }
          else {
            Future(Left(dropResult.left.get))
        })
    })
  }

  private[this] def getManagement: Option[AhcManagementClient] = {
    def influx = InfluxMng(influxAddress, influxPort, Some(influxCredentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }

  private[this] def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private[this] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[this] def destinations: Seq[String] = Seq(s"$listenProtocol://$listenAddress:$listenPort")
}
