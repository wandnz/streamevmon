package nz.net.wand

import nz.net.wand.measurements.ICMP

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.{InetAddress, ServerSocket, SocketTimeoutException}

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.sys.ShutdownHookThread
import scala.util.{Failure, Success, Try}

class ICMPMeasurementSourceFunction(
  subscriptionName: String = "SubscriptionServer",
  dbName          : String = "nntsc",
  rpName          : String = "nntscdefault",
  protocol        : String = "http",
  listenAddress   : String = "130.217.250.59",
  listenPort      : Int = 8008,
  listenBacklog   : Int = 5,
  influxAddress   : String = "localhost",
  influxPort      : Int = 8086,
  influxUsername  : String = "cuz",
  influxPassword  : String = ""
) extends SourceFunction[ICMP]
    with Logging {

  private[this] var isRunning = false

  private[this] var influx: Option[AhcManagementClient] = Option.empty

  private[this] var listener: Option[ServerSocket] = Option.empty

  override def run(ctx: SourceFunction.SourceContext[ICMP]): Unit =
  {
    var shutdownHooks: Seq[ShutdownHookThread] = Seq()

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
            addOrUpdateSubscription().flatMap(subscribeResult =>
              if (subscribeResult.isLeft) {
                Future(logger.error(s"Failed to update subscription: ${subscribeResult.left.get}"))
              }
              else {
                logger.info(s"Added subscription $subscriptionName")
                shutdownHooks = shutdownHooks :+ sys.addShutdownHook {
                  Await.result(dropSubscription(), Duration.Inf)
                  logger.info(s"Removed subscription $subscriptionName")
                }
                Future(listen(ctx))
            })

          Await.result(subscribeFuture, Duration.Inf)
      }
    }

    shutdownHooks.foreach { hook =>
      logger.debug("Shutting down fully")
      hook.run()
      hook.remove()
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

  private[this] def listen(ctx: SourceFunction.SourceContext[ICMP]): Unit =
  {
    logger.info("Listening for subscribed events...")

    isRunning = true
    while (isRunning)
    {
      try
      {
        listener match
        {
          case Some(serverSock) =>
            val sock = serverSock.accept
            sock.setSoTimeout(100)
            val reader = new BufferedReader(new InputStreamReader(sock.getInputStream))

            Stream
              .continually
              {
                val in = reader.readLine
                if (in != null)
                {
                  val result = ICMP.Create(in)
                  result match
                  {
                    case Some(x) => ctx.collect(x)
                    case None =>
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
      }
      catch
      {
        case _: SocketTimeoutException =>
      }
    }

    logger.info("No longer listening")
  }

  private[this] def startListener(): Try[Unit] =
  {
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

  private[this] def connectInflux(): Boolean =
  {
    influx = Some(InfluxMng(influxAddress, influxPort, Some(influxCredentials)))

    checkInfluxConnection()
  }

  private[this] def disconnectInflux(): Unit =
    influx match {
      case Some(db) => db.close
      case None     =>
    }

  private[this] def checkInfluxConnection(): Boolean =
    influx match {
      case Some(db) =>
        Await.result(db.ping.map(result => result.isRight), Duration.Inf)
      case None => false
    }

  private[this] def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] =
    influx match {
      case Some(_) =>
        addSubscription().flatMap(addResult =>
          if (addResult.isRight) {
            // Successfully added
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
      case None =>
        Future(Left(new IllegalStateException("No database connection")))
    }

  private[this] def addSubscription(): Future[ErrorOr[ResponseCode]] =
    influx match {
      case Some(db) =>
        db.createSubscription(
          subscriptionName,
          dbName,
          rpName,
          Destinations.ALL,
          destinations
        )
      case None =>
        Future(Left(new IllegalStateException("No database connection")))
    }

  private[this] def dropSubscription(): Future[ErrorOr[ResponseCode]] =
    influx match {
      case Some(db) => db.dropSubscription(subscriptionName, dbName, rpName)
      case None =>
        Future(Left(new IllegalStateException("No database connection")))
    }
}
