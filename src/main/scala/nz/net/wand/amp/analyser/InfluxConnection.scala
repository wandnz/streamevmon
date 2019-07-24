package nz.net.wand.amp.analyser

import com.github.fsanaulla.chronicler.ahc.io.{AhcIOClient, InfluxIO}
import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.ahc.shared.InfluxAhcClient
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread

object InfluxConnection extends Logging {

  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  def checkConnection(influx: AhcIOClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  def getManagement(address: String,
                    port: Int,
                    credentials: InfluxCredentials): Option[AhcManagementClient] = {
    def influx = InfluxMng(address, port, Some(credentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }

  def getIO(address: String, port: Int, credentials: InfluxCredentials): Option[AhcIOClient] = {
    def influx = InfluxIO(address, port, Some(credentials))

    if (checkConnection(influx)) {
      Some(influx)
    }
    else {
      None
    }
  }

  def disconnect(influx: InfluxAhcClient): Unit = {
    influx.close()
  }

  def addSubscription(influx: AhcManagementClient,
                      subscriptionName: String,
                      dbName: String,
                      rpName: String,
                      destinations: Seq[String]): Future[ErrorOr[ResponseCode]] = {

    influx
      .createSubscription(
        subscriptionName,
        dbName,
        rpName,
        Destinations.ALL,
        destinations
      )
      .flatMap { subscribeResult =>
        if (subscribeResult.isRight) {
          subscriptionRemoveHooks = subscriptionRemoveHooks :+ (subscriptionName, sys
            .addShutdownHook {
              Await.result(dropSubscription(influx, subscriptionName, dbName, rpName), Duration.Inf)
              logger.info(s"Removed subscription $subscriptionName")
            })
          logger.info(s"Added subscription $subscriptionName")
          Future(Right(subscribeResult.right.get))
        }
        else {
          Future(Left(subscribeResult.left.get))
        }
      }
  }

  def dropSubscription(influx: AhcManagementClient,
                       subscriptionName: String,
                       dbName: String,
                       rpName: String): Future[ErrorOr[ResponseCode]] = {

    subscriptionRemoveHooks.filter(_._1 == subscriptionName).foreach { hook =>
      if (!hook._2.isAlive) { hook._2.remove }
    }
    influx.dropSubscription(subscriptionName, dbName, rpName)
  }

  def addOrUpdateSubscription(influx: AhcManagementClient,
                              subscriptionName: String,
                              dbName: String,
                              rpName: String,
                              destinations: Seq[String]): Future[ErrorOr[ResponseCode]] = {
    addSubscription(influx, subscriptionName, dbName, rpName, destinations).flatMap(addResult =>
      if (addResult.isRight) {
        // Successfully added
        Future(addResult)
      }
      else {
        dropSubscription(influx, subscriptionName, dbName, rpName).flatMap(dropResult =>
          if (dropResult.isRight) {
            addSubscription(influx, subscriptionName, dbName, rpName, destinations)
          }
          else {
            Future(Left(dropResult.left.get))
        })
    })
  }
}
