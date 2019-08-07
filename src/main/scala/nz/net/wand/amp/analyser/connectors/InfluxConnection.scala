package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.{Configuration, Logging}

import java.io.IOException
import java.net.{ConnectException, InetAddress, ServerSocket}

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.configuration.IllegalConfigurationException

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread

object InfluxConnection extends Logging with Configuration {

  configPrefix = "connectors.influx.dataSource"
  var subscriptionName: String = getConfigString("subscriptionName").getOrElse("SubscriptionServer")
  var dbName: String = getConfigString("databaseName").getOrElse("nntsc")
  var rpName: String = getConfigString("retentionPolicyName").getOrElse("nntscdefault")
  var listenProtocol: String = getConfigString("listenProtocol").getOrElse("http")

  var listenAddress: String =
    getConfigString("listenAddress").getOrElse {
      throw new IllegalConfigurationException(s"You must specify $configPrefix.listenAddress")
    }
  var listenPort: Int = getConfigInt("listenPort").getOrElse(8008)
  var listenBacklog: Int = getConfigInt("listenBacklog").getOrElse(5)
  var influxAddress: String = getConfigString("serverName").getOrElse("localhost")
  var influxPort: Int = getConfigInt("portNumber").getOrElse(8086)
  var influxUsername: String = getConfigString("user").getOrElse("cuz")
  var influxPassword: String = getConfigString("password").getOrElse("")

  var influx: Option[AhcManagementClient] = None
  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) =>
        true
      case Left(_) =>
        false
    }, Duration.Inf)
  }

  private[this] def ensureConnection(): Unit = {
    influx match {
      case Some(_) =>
      case None    => influx = getManagement
    }
  }

  def disconnect(): Unit = {
    influx.foreach(_.close)
    influx = None
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
                  logger.debug(s"Removed subscription $subscriptionName")
                })
              logger.debug(s"Added subscription $subscriptionName at ${destinations.mkString(",")}")
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
            try {
              hook._2.remove
            } catch {
              case _: IllegalStateException => // Already shutting down, so the hook will run.
            }
          }
        }
        logger.debug(s"Dropping subscription $subscriptionName")
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

  def destinations: Seq[String] = Seq(s"$listenProtocol://$listenAddress:$listenPort")

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
}
