package nz.net.wand.amp.analyser

import java.io.{BufferedReader, InputStreamReader, IOException}
import java.net.{InetAddress, ServerSocket, SocketTimeoutException}

import com.github.fsanaulla.chronicler.ahc.management.AhcManagementClient
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

abstract class InfluxSubscriptionSourceFunction[T]()
    extends SourceFunction[T]
    with Logging
    with Configuration {

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

  private[this] var isRunning = false
  private[this] var influx: Option[AhcManagementClient] = Option.empty
  private[this] var listener: Option[ServerSocket] = Option.empty

  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    if (!connectInflux()) {
      logger.error(s"Failed to connect to influx")
    }
    else {
      logger.info("Connected to influx")
      startListener() match {
        case Failure(ex) => logger.error(s"Failed to start listener: $ex")
        case Success(_) =>
          logger.info("Started listener")
          val subscribeFuture =
            addOrUpdateSubscription().flatMap {
              case Right(_) => Future(listen(ctx))
              case Left(error) =>
                Future(logger.error(s"Failed to subscribe to InfluxDB stream: $error"))
            }

          Await.result(subscribeFuture, Duration.Inf)
      }
    }

    stopListener()
    logger.info("Stopped listener")
    disconnectInflux()
    logger.info("Disconnected from Influx")
  }

  override def cancel(): Unit = {
    logger.info("Stopping listener...")
    isRunning = false
  }

  protected[this] def processLine(ctx: SourceFunction.SourceContext[T], line: String): Unit

  private[this] def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private[this] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[this] def destinations: Seq[String] = Seq(s"$listenProtocol://$listenAddress:$listenPort")

  private[this] def listen(ctx: SourceFunction.SourceContext[T]): Unit = {
    logger.info("Listening for subscribed events...")

    isRunning = true
    while (isRunning) {
      try {
        listener match {
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

            Stream
              .continually {
                val in = reader.readLine
                processLine(ctx, in)
                in
              }
              .takeWhile(line => line != null)
              .foreach(line => line)

          case None =>
            logger.warn("Listener unexpectedly died")
            isRunning = false
        }
      } catch {
        case _: SocketTimeoutException =>
      }
    }

    logger.info("No longer listening")
  }

  private[this] def startListener(): Try[Unit] = {
    try {
      listener = Some(new ServerSocket(listenPort, listenBacklog, listenInet))
      listener.get.setSoTimeout(100)
      Success(Unit)
    } catch {
      case ex @ (_: SecurityException | _: IOException | _: IllegalArgumentException) =>
        Failure(ex.getClass.getConstructor().newInstance("Error starting listener: ", ex))
    }
  }

  private[this] def stopListener(): Unit =
    listener match {
      case Some(listen) => listen.close()
      case None         =>
    }

  private[this] def connectInflux(): Boolean = {
    influx = InfluxConnection.getManagement(influxAddress, influxPort, influxCredentials)
    influx.isDefined
  }

  private[this] def disconnectInflux(): Unit =
    influx match {
      case Some(db) => InfluxConnection.disconnect(db)
      case None     =>
    }

  private[this] def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] =
    influx match {
      case Some(db) =>
        InfluxConnection.addOrUpdateSubscription(db, subscriptionName, dbName, rpName, destinations)
      case None =>
        Future(Left(new IllegalStateException("No database connection")))
    }
}
