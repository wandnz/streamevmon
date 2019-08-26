package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.Logging

import java.io.IOException
import java.net._
import java.util.Collections

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread

/** Contains additional apply methods for the companion class.
  */
object InfluxConnection {

  /** Gets an IPV4 address which should be visible to other hosts. Prefers
    * addresses that aren't "site-local", meaning they are outside of the
    * reserved private address space.
    */
  private[connectors] def getListenAddress: String = {
    try {
      val nonLoopbacks = JavaConverters.asScalaBufferConverter(Collections.list(NetworkInterface.getNetworkInterfaces)).asScala.flatMap {
        iface => JavaConverters.asScalaBufferConverter(Collections.list(iface.getInetAddresses)).asScala
      }
        .filter(_.isInstanceOf[Inet4Address])
        .filterNot(_.isLinkLocalAddress)
        .filterNot(_.isLoopbackAddress)

      // This can probably be achieved by specifying a custom sort order that
      // will sort site-local addresses below globally routable addresses.
      val globals = nonLoopbacks.filterNot(_.isSiteLocalAddress)
      if (globals.nonEmpty) {
        globals.map(_.getHostAddress).head
      }
      else {
        nonLoopbacks.map(_.getHostAddress)
          .headOption
          .getOrElse("localhost")
      }
    }
    catch {
      case e: SocketException => println(s"couldn't get IP! $e")
        "localhost"
    }
  }

  private[this] def getOrFindListenAddress(address: String): String = {
    if (address == null) {
      getListenAddress
    }
    else {
      address
    }
  }

  /** Creates a new InfluxConnection from the given config. Expects all fields
    * specified in the companion class' main documentation to be present.
    *
    * @param p The configuration to use. Generally obtained from the Flink
    *          global configuration.
    * @param configPrefix A custom config prefix to use, in case the configuration
    *                     object is not as expected.
    *
    * @return A new InfluxConnection object.
    */
  def apply(p: ParameterTool, configPrefix: String = "influx.dataSource"): InfluxConnection =
    InfluxConnection(
      p.get(s"$configPrefix.subscriptionName"),
      p.get(s"$configPrefix.databaseName"),
      p.get(s"$configPrefix.retentionPolicyName"),
      p.get(s"$configPrefix.listenProtocol"),
      getOrFindListenAddress(p.get(s"$configPrefix.listenAddress")),
      p.getInt(s"$configPrefix.listenPort"),
      p.getInt(s"$configPrefix.listenBacklog"),
      p.get(s"$configPrefix.serverName"),
      p.getInt(s"$configPrefix.portNumber"),
      p.get(s"$configPrefix.user"),
      p.get(s"$configPrefix.password")
    )
}

/** InfluxDB subscription manager which produces corresponding ServerSockets.
  *
  * Used in [[nz.net.wand.amp.analyser.flink.InfluxSubscriptionSourceFunction InfluxSubscriptionSourceFunction]].
  *
  * ==Configuration==
  *
  * This class is configured by the `influx.dataSource` config key group.
  *
  * - `listenAddress`: The address to listen on for this subscription.
  * If not specified, this will be automatically generated at runtime by
  * inspecting the IP addresses attached to the interfaces on the host machine.
  * If a non-loopback, non-link-local address is found, the program will bind to
  * it. If several are found, it will prefer any that are not in restricted
  * private IP ranges. Specify this option if the automatic selection does not
  * fit your needs.
  *
  * - `listenPort`: The port this program should listen on.
  * Defaults to an ephemeral port if no configuration is supplied or if the desired port cannot be bound.
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
  * "http", "https", or "udp", although https and udp have not been tested.
  * Default "http".
  *
  * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html]]
  * @see [[nz.net.wand.amp.analyser.flink.InfluxSinkFunction InfluxSinkFunction]]
  * @see [[PostgresConnection]]
  */
case class InfluxConnection(
  subscriptionName: String,
  dbName          : String,
  rpName          : String,
  listenProtocol  : String,
  listenAddress   : String,
  listenPort      : Int,
  listenBacklog   : Int,
  influxAddress   : String,
  influxPort      : Int,
  influxUsername  : String,
  influxPassword  : String
) extends Logging {

  private[this] var obtainedPort: Int = 0

  /** The value of the DESTINATIONS field of the subscription that is created
    * in InfluxDB.
    */
  private[connectors] def destinations: Seq[String] =
    Seq(s"$listenProtocol://$listenAddress:$obtainedPort")

  private[connectors] def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private[connectors] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[connectors] lazy val influx: Option[AhcManagementClient] = {
    getManagement
  }

  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  /** Disconnect the InfluxDB connection.
    */
  def disconnect(): Unit = {
    influx.foreach(_.close)
  }

  /** Gets a new ServerSocket with the requested configuration. If the desired
    * port cannot be bound, tries again with an ephemeral port.
    *
    * @return The ServerSocket if successful, or None.
    */
  private[this] def getServerSocket: Option[ServerSocket] = {
    // Try get a socket with the specified or default port
    try {
      val r = new ServerSocket(listenPort, listenBacklog, listenInet)
      r.setSoTimeout(100)
      obtainedPort = r.getLocalPort
      Some(r)
    }
    // If the port couldn't be bound, try with an ephemeral port
    catch {
      case _: IOException =>
        try {
          val r = new ServerSocket(0, listenBacklog, listenInet)
          r.setSoTimeout(100)
          obtainedPort = r.getLocalPort
          Some(r)
        }
        // If we still can't manage, give up.
        catch {
          case e: Exception =>
            logger.error(s"Could not create ServerSocket with configuration $listenPort, $listenBacklog, ${listenInet.getHostAddress}: $e")
            None
        }
      case e: Exception =>
        logger.error(s"Could not create ServerSocket with configuration $listenPort, $listenBacklog, ${listenInet.getHostAddress}: $e")
        None
    }
  }

  /** Starts a listener corresponding to a new InfluxDB subscription.
    *
    * Should be closed with [[stopSubscriptionListener]].
    *
    * @return The socket to listen on if the connection was successful and the
    *         socket could be created, otherwise None.
    */
  def getSubscriptionListener: Option[ServerSocket] = {
    val ssock = getServerSocket
    ssock match {
      case None => None
      case Some(x) =>
        val addOrUpdateResult = addOrUpdateSubscription().map {
          case Right(_) => Some(x)
          case Left(ex) =>
            logger.error("Error starting listener: " + ex)
            None
        }
        Await.result(addOrUpdateResult, Duration.Inf)
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
                  try {
                    Await.result(dropSubscription(), Duration.Inf)
                    logger.debug(s"Removed subscription $subscriptionName")
                  }
                  catch {
                    case ex@(_: Exception) =>
                      logger.debug(
                        s"Could not remove subscription $subscriptionName: ${ex.getMessage}")
                  }
                })
              logger.info(s"Added subscription $subscriptionName at ${destinations.mkString(",")}")
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
    *
    * @return Whether the connection can ping InfluxDB.
    */
  private[this] def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_) => false
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
