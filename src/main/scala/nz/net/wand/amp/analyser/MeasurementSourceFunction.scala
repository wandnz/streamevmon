package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.measurements.{Measurement, MeasurementFactory}

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.{InetAddress, ServerSocket, SocketTimeoutException}

import com.github.fsanaulla.chronicler.ahc.management.AhcManagementClient
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class MeasurementSourceFunction(
    subscriptionName: String = "SubscriptionServer",
    dbName: String = "nntsc",
    rpName: String = "nntscdefault",
    protocol: String = "http",
    listenAddress: String = "130.217.250.59",
    listenPort: Int = 8008,
    listenBacklog: Int = 5,
    influxAddress: String = "localhost",
    influxPort: Int = 8086,
    influxUsername: String = "cuz",
    influxPassword: String = ""
) extends SourceFunction[Measurement]
    with Logging {

  private[this] var isRunning = false

  private[this] var influx: Option[AhcManagementClient] = Option.empty

  private[this] var listener: Option[ServerSocket] = Option.empty

  override def run(ctx: SourceFunction.SourceContext[Measurement]): Unit = {
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

  private[this] def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  private[this] def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  private[this] def destinations: Seq[String] = Seq(s"$protocol://$listenAddress:$listenPort")

  private[this] def listen(ctx: SourceFunction.SourceContext[Measurement]): Unit = {
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
                if (in != null) {
                  val result = MeasurementFactory.createMeasurement(in)
                  result match {
                    case Some(x) => ctx.collect(x)
                    case None    =>
                  }
                }
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
