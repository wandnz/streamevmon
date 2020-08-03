package nz.net.wand.streamevmon.connectors.influx

import nz.net.wand.streamevmon.Logging

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

/** Contains additional constructors and private methods for the companion class.
  */
object InfluxConnection extends Logging {

  /** Gets an IPV4 address which should be visible to other hosts. Prefers
    * addresses that aren't "site-local", meaning they are outside of the
    * reserved private address space.
    */
  def getListenAddress: String = {
    try {
      val nonLoopbacks = JavaConverters
        .asScalaBufferConverter(Collections.list(NetworkInterface.getNetworkInterfaces))
        .asScala
        .flatMap { iface =>
          JavaConverters.asScalaBufferConverter(Collections.list(iface.getInetAddresses)).asScala
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
        nonLoopbacks
          .map(_.getHostAddress)
          .headOption
          .getOrElse("localhost")
      }
    }
    catch {
      case e: SocketException =>
        logger.error(s"couldn't get IP! $e")
        "localhost"
    }
  }

  def getOrFindListenAddress(address: String): String = {
    if (address == null) {
      getListenAddress
    }
    else {
      address
    }
  }

  private[this] def getWithFallback(p: ParameterTool, configPrefix: String, datatype: String, item: String): String = {
    val result = p.get(s"source.$configPrefix.$datatype.$item", null)
    if (result == null) {
      p.get(s"source.$configPrefix.$item")
    }
    else {
      result
    }
  }

  /** Creates a new InfluxConnection from the given config. Expects all fields
    * specified in the companion class' main documentation to be present.
    *
    * @param p            The configuration to use. Generally obtained from the Flink
    *                     global configuration.
    * @param configPrefix A custom config prefix to use, in case the configuration
    *                     object is not as expected.
    *
    * @return A new InfluxConnection object.
    */
  def apply(p: ParameterTool, configPrefix: String = "influx", datatype: String = "amp"): InfluxConnection = {
    if (getWithFallback(p, configPrefix, datatype, "subscriptionName") == null) {
      throw new IllegalArgumentException(s"$configPrefix.$datatype.subscriptionName cannot be null!")
    }
    InfluxConnection(
      getWithFallback(p, configPrefix, datatype, "subscriptionName"),
      getWithFallback(p, configPrefix, datatype, "databaseName"),
      getWithFallback(p, configPrefix, datatype, "retentionPolicy"),
      getWithFallback(p, configPrefix, datatype, "listenProtocol"),
      getOrFindListenAddress(getWithFallback(p, configPrefix, datatype, "listenAddress")),
      getWithFallback(p, configPrefix, datatype, "listenPort").toInt,
      getWithFallback(p, configPrefix, datatype, "listenBacklog").toInt,
      getWithFallback(p, configPrefix, datatype, "serverName"),
      getWithFallback(p, configPrefix, datatype, "portNumber").toInt,
      getWithFallback(p, configPrefix, datatype, "user"),
      getWithFallback(p, configPrefix, datatype, "password")
    )
  }
}

/** InfluxDB subscription manager which produces ServerSockets. See the package
  * object for details on how to configure this class.
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

  private var obtainedPort: Int = 0

  /** The value of the DESTINATIONS field of the subscription that is created
    * in InfluxDB.
    */
  def destinations: Array[String] = Array(s"$listenProtocol://$listenAddress:$obtainedPort")

  private def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  protected[connectors] lazy val influx: Option[AhcManagementClient] = {
    getManagement
  }

  private var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

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
  protected def getServerSocket: Option[ServerSocket] = {
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
            logger.error(
              s"Could not create ServerSocket with configuration $listenPort, $listenBacklog, ${listenInet.getHostAddress}: $e")
            None
        }
      case e: Exception =>
        logger.error(
          s"Could not create ServerSocket with configuration $listenPort, $listenBacklog, ${listenInet.getHostAddress}: $e")
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
  protected[connectors] def addSubscription(): Future[ErrorOr[ResponseCode]] = {
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
                    logger.debug(s"Removed subscription $subscriptionName on $dbName at $influxAddress:$influxPort")
                  }
                  catch {
                    case ex: Exception =>
                      logger.debug(
                        s"Could not remove subscription $subscriptionName on $dbName at $influxAddress:$influxPort: ${ex.getMessage}")
                  }
                })
              logger.info(s"Added subscription $subscriptionName on $dbName at $influxAddress:$influxPort to ${destinations.mkString(",")}")
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
  protected[connectors] def dropSubscription(): Future[ErrorOr[ResponseCode]] = {
    influx match {
      case Some(db) =>
        subscriptionRemoveHooks.filter(_._1 == subscriptionName).foreach { hook =>
          if (!hook._2.isAlive) {
            try {
              hook._2.remove
            }
            catch {
              case _: IllegalStateException => // Already shutting down, so the hook will run.
            }
          }
        }
        logger.debug(s"Dropping subscription $subscriptionName on $dbName at $influxAddress:$influxPort")
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
  protected[connectors] def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] = {
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
  protected def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(e) => throw e
    }, Duration.Inf)
  }

  /** Gets an InfluxDB management client according to the current configuration.
    *
    * @return A management client if the connection was valid, otherwise None.
    */
  protected def getManagement: Option[AhcManagementClient] = {
    def influx: AhcManagementClient = InfluxMng(influxAddress, influxPort, Some(influxCredentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }
}
