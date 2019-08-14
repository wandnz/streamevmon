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

/** InfluxDB subscription manager which produces corresponding ServerSockets.
  *
  * Used in [[nz.net.wand.amp.analyser.flink.InfluxSubscriptionSourceFunction InfluxSubscriptionSourceFunction]].
  *
  * ==Configuration==
  *
  * This class is configured by the `connectors.influx.dataSource` config key group.
  *
  * - `listenAddress`: '''Required'''. The address to listen on for this subscription.
  *
  * - `listenPort`: The port this program should listen on.
  * Default 8008.
  *
  * - `listenBacklog`: The requested maximum length of the queue of incoming connections.
  * Default 5.
  * See [[https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html ServerSocket]] documentation.
  *
  * - `serverName`: The address that InfluxDB can be found at.
  * Default "localhost".
  *
  * - `portNumber`: The port that InfluxDB is listening on.
  * Default 8086.
  *
  * - `user`: The username that should be used to connect to InfluxDB.
  * Default "cuz"
  *
  * - `password`: The password that should be used to connect to InfluxDB.
  * Default "".
  *
  * - `subscriptionName`: The name of the subscription that will be created in InfluxDB.
  * Default "SubscriptionServer".
  *
  * - `databaseName`: The name of the InfluxDB database to subscribe to.
  * Default "nntsc".
  *
  * - `retentionPolicyName`: The name of the InfluxDB retention policy to subscribe to.
  * Default "nntscdefault".
  *
  * - `listenProtocol`: The transport protocol for this subscription. Can be one of
  * "http", "https", or "udp".
  * Default "http".
  *
  * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html]]
  * @see [[nz.net.wand.amp.analyser.flink.InfluxSinkFunction InfluxSinkFunction]]
  * @see [[PostgresConnection]]
  */
object InfluxConnection extends Configuration with Logging {

  configPrefix = "connectors.influx.dataSource"

  private[connectors] var subscriptionName: String =
    getConfigString("subscriptionName").getOrElse("SubscriptionServer")

  private[connectors] var dbName: String = getConfigString("databaseName").getOrElse("nntsc")

  private[connectors] var rpName: String =
    getConfigString("retentionPolicyName").getOrElse("nntscdefault")

  private[connectors] var listenProtocol: String =
    getConfigString("listenProtocol").getOrElse("http")

  private[connectors] var listenAddress: String =
    getConfigString("listenAddress").getOrElse {
      throw new IllegalConfigurationException(s"You must specify $configPrefix.listenAddress")
    }

  // TODO: This should default to an automatically selected port.
  private[connectors] var listenPort: Int = getConfigInt("listenPort").getOrElse(8008)

  /** The requested maximum length of the queue of incoming connections
    */
  private[connectors] var listenBacklog: Int = getConfigInt("listenBacklog").getOrElse(5)

  /** The address that InfluxDB can be found at. */
  private[connectors] var influxAddress: String =
    getConfigString("serverName").getOrElse("localhost")

  /** The port that InfluxDB is listening on. */
  private[connectors] var influxPort: Int = getConfigInt("portNumber").getOrElse(8086)

  /** The username that should be used to connect to InfluxDB. */
  private[connectors] var influxUsername: String = getConfigString("user").getOrElse("cuz")

  /** The password that should be used to connect to InfluxDB. */
  private[connectors] var influxPassword: String = getConfigString("password").getOrElse("")

  /** The value of the DESTINATIONS field of the subscription that is created
    * in InfluxDB.
    */
  private[connectors] def destinations: Seq[String] =
    Seq(s"$listenProtocol://$listenAddress:$listenPort")

  private[connectors] def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private[connectors] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[connectors] var influx: Option[AhcManagementClient] = None

  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  /** Ensure that the InfluxDB connection exists and is valid.
    */
  private[this] def ensureConnection(): Unit = {
    influx match {
      case Some(_) =>
      case None    => influx = getManagement
    }
  }

  /** Disconnect the InfluxDB connection.
    */
  def disconnect(): Unit = {
    influx.foreach(_.close)
    influx = None
  }

  /** Starts a listener corresponding to a new InfluxDB subscription.
    *
    * Should be closed with [[stopSubscriptionListener]].
    *
    * @return The socket to listen on if the connection was successful and the
    *         socket could be created, otherwise None.
    */
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

  /** Stops a listener and its corresponding InfluxDB subscription.
    *
    * @param ssock A server socket, as created by [[getSubscriptionListener]].
    */
  def stopSubscriptionListener(ssock: ServerSocket): Unit = {
    Await.result(dropSubscription(), Duration.Inf)
    ssock.close()
  }

  /** Creates a new InfluxDB subscription according to the current configuration.
    *
    * @return A Future of either an error or the response from InfluxDB.
    */
  private[connectors] def addSubscription(): Future[ErrorOr[ResponseCode]] = {
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

  /** Stops an InfluxDB subscription according to the current configuration.
    *
    * @return A Future of either an error or the response from InfluxDB.
    */
  private[connectors] def dropSubscription(): Future[ErrorOr[ResponseCode]] = {
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

  /** Forcefully creates a new InfluxDB subscription according to the current
    * configuration. Overwrites any existing subscriptions with the same name
    * on the same database and retention policy.
    *
    * @return A Future of either an error or the response from InfluxDB.
    */
  private[connectors] def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] = {
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

  /** Checks an InfluxDB connection.
    *
    * @param influx The InfluxDB connection to check.
    * @return Whether the connection can ping InfluxDB.
    */
  private[this] def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) =>
        true
      case Left(_) =>
        false
    }, Duration.Inf)
  }

  /** Gets an InfluxDB management client according to the current configuration.
    *
    * @return A management client if the connection was valid, otherwise None.
    */
  private[this] def getManagement: Option[AhcManagementClient] = {
    def influx = InfluxMng(influxAddress, influxPort, Some(influxCredentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }
}
