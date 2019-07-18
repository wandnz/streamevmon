package nz.ac.waikato

import java.io.IOException
import java.net.{InetAddress, ServerSocket}

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.sys.ShutdownHookThread
import scala.util.{Failure, Success, Try}

class ICMPMeasurementSourceFunction(subscriptionName: String = "SubscriptionServer",
                                    dbName: String = "nntsc",
                                    rpName: String = "nntscdefault",
                                    protocol: String = "http",
                                    listenAddress: String = "130.217.250.59",
                                    listenPort: Int = 8008,
                                    listenBacklog: Int = 5,
                                    influxAddress: String = "localhost",
                                    influxPort: Int = 8086,
                                    influxUsername: String = "cuz",
                                    influxPassword: String = "")
    extends SourceFunction[ICMPMeasurement]
    with Logging {

  var isRunning = false

  var influx: Option[AhcManagementClient] = Option.empty

  var listener: Option[ServerSocket] = Option.empty

  def listenInet: InetAddress = InetAddress.getByName(listenAddress)

  def influxCredentials = InfluxCredentials(influxUsername, influxPassword)

  def destinations: Seq[String] = Seq(s"$protocol://$listenAddress:$listenPort")

  def listen(): Unit = {
    logger.info("I'm definitely listening right now")
  }

  override def run(ctx: SourceFunction.SourceContext[ICMPMeasurement]): Unit = {
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
                  Await.ready(dropSubscription(), Duration.Inf)
                  logger.info(s"Removed subscription $subscriptionName")
                }
                Future(listen())
            })

          Await.ready(subscribeFuture, Duration.Inf)
      }
    }

    shutdownHooks.foreach { hook =>
      hook.run()
      hook.remove()
    }

    disconnectInflux()
    stopListener()
  }

  override def cancel(): Unit = {
    isRunning = false
  }

  def startListener(): Try[Unit] = {
    try {
      listener = Some(new ServerSocket(listenPort, listenBacklog, listenInet))
      listener.get.setSoTimeout(100)
      Success(Unit)
    } catch {
      case ex @ (_: SecurityException | _: IOException | _: IllegalArgumentException) =>
        Failure(ex.getClass.getConstructor().newInstance("Error starting listener: ", ex))
    }
  }

  def stopListener(): Unit =
    listener match {
      case Some(listen) => listen.close()
      case None         =>
    }

  def connectInflux(): Boolean = {
    influx = Some(InfluxMng(influxAddress, influxPort, Some(influxCredentials)))

    checkInfluxConnection()
  }

  def disconnectInflux(): Unit =
    influx match {
      case Some(db) => db.close
      case None     =>
    }

  def checkInfluxConnection(): Boolean =
    influx match {
      case Some(db) =>
        val ping = db.ping
        var pingResult: Boolean = false

        ping.onComplete {
          case Success(_) => pingResult = true
          case Failure(_) => pingResult = false
        }

        Await.ready(ping, Duration.Inf)

        pingResult

      case None => false
    }

  def addOrUpdateSubscription(): Future[ErrorOr[ResponseCode]] =
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

  def addSubscription(): Future[ErrorOr[ResponseCode]] =
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

  def dropSubscription(): Future[ErrorOr[ResponseCode]] =
    influx match {
      case Some(db) => db.dropSubscription(subscriptionName, dbName, rpName)
      case None =>
        Future(Left(new IllegalStateException("No database connection")))
    }
}
