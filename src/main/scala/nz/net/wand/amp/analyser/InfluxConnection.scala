package nz.net.wand.amp.analyser

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.ahc.shared.InfluxAhcClient
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.sys.ShutdownHookThread

object InfluxConnection {

  private[this] var subscriptionRemoveHooks: Seq[(String, ShutdownHookThread)] = Seq()

  def checkConnection(influx: AhcManagementClient): Boolean = {
    Await.result(influx.ping.map {
      case Right(_) => true
      case Left(_)  => false
    }, Duration.Inf)
  }

  def getManagement(influxAddress: String,
                    influxPort: Int,
                    influxCredentials: InfluxCredentials): Option[AhcManagementClient] = {
    def influx = InfluxMng(influxAddress, influxPort, Some(influxCredentials))

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
            })
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