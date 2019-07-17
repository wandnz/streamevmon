package nz.ac.waikato

import java.io._
import java.net.ServerSocket

import com.github.fsanaulla.chronicler.ahc.management.{AhcManagementClient, InfluxMng}
import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.enums.Destinations
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class SubscriptionServer(port: Int = 8008) extends Thread with Logging
{
  val subscriptionName: String = "SubscriptionServer"
  val dbName: String = "nntsc"
  val rpName = "nntscdefault"
  val protocol = "http://"
  val localBindAddress = "130.217.250.59"

  val influxAddress = "localhost"
  val influxPort = 8086
  val influxCredentials = InfluxCredentials("cuz", "")

  val destinationStrings: Seq[String] = Seq(s"$protocol$localBindAddress:$port")

  override def run(): Unit =
  {
    val server = new ServerSocket(port)
    val influxDB = InfluxMng(influxAddress, influxPort, Some(influxCredentials))

    checkConnectionOrDie(influxDB)
    addOrUpdateSubscription(influxDB)

    sys.addShutdownHook(Await.ready(dropSubscription(influxDB), Duration.Inf))

    while (true)
    {
      val reader =
        new BufferedReader(
          new InputStreamReader(
            server.accept.getInputStream))

      val lines = Stream.continually(reader.readLine).takeWhile(_ != null)

      ICMPFactory.CreateICMPs(lines).foreach(l => logger.info(l))

      dropSubscription(influxDB)
      reader.close()
    }
  }

  def checkConnectionOrDie(influxDB: AhcManagementClient): Unit =
  {
    val pingFuture = influxDB.ping
    pingFuture.onComplete
    {
      case Success(_) => logger.info(s"Successfully connected to InfluxDB")
      case Failure(exception) =>
        logger.error(s"Failed to connect: $exception")
        System.exit(1)
    }
    Await.ready(pingFuture, Duration.Inf)
  }

  def addOrUpdateSubscription(influxDB: AhcManagementClient): Unit =
  {
    val subscribeFuture = addSubscription(influxDB).flatMap(addResult =>
    {
      if (addResult.isRight)
      {
        Future(logger.info("Added subscription"))
      }
      else
      {
        dropSubscription(influxDB).flatMap(dropResult =>
        {
          if (dropResult.isRight)
          {
            addSubscription(influxDB).flatMap(updateResult =>
            {
              if (updateResult.isRight)
              {
                Future(logger.info("Updated subscription"))
              }
              else
              {
                Future(logger.error(s"Failed to re-add subscription: ${updateResult.left.get}")).flatMap(_ => Future(System.exit(1)))
              }
            })
          }
          else
          {
            Future(logger.error(s"Failed to drop subscription: ${dropResult.left.get}")).flatMap(_ => Future(System.exit(1)))
          }
        })
      }
    })

    Await.ready(subscribeFuture, Duration.Inf)
  }

  def addSubscription(influxDB: AhcManagementClient): Future[ErrorOr[ResponseCode]] =
  {
    influxDB.createSubscription(
      subscriptionName,
      dbName,
      rpName,
      Destinations.ALL,
      destinationStrings
    )
  }

  def dropSubscription(influxDB: AhcManagementClient): Future[ErrorOr[ResponseCode]] =
  {
    influxDB.dropSubscription(
      subscriptionName,
      dbName,
      rpName
    )
  }
}

object InfluxSubscription
{
  def main(args: Array[String]): Unit =
  {
    val env = ExecutionEnvironment.getExecutionEnvironment

    val oneRequest = env.fromElements("POST /write?consistency=&db=nntsc&precision=ns&rp=nntscdefault HTTP/1.1\nHost: 130.217.250.59:2000\nUser-Agent: InfluxDBClient\nContent-Length: 1190\nContent-Type: \nAccept-Encoding: gzip\n\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=218i,packet_size=367i,results=1i,rtts=\"[218]\" 1563311880000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36038i,packet_size=367i,results=1i,rtts=\"[36038]\" 1563311880000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=178i,packet_size=552i,results=1i,rtts=\"[178]\" 1563311940000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36148i,packet_size=552i,results=1i,rtts=\"[36148]\" 1563311940000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=810i,packet_size=216i,results=1i,rtts=\"[810]\" 1563312000000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36764i,packet_size=216i,results=1i,rtts=\"[36764]\" 1563312000000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=236i,packet_size=391i,results=1i,rtts=\"[236]\" 1563312060000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36360i,packet_size=391i,results=1i,rtts=\"[36360]\" 1563312060000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=357i,packet_size=572i,results=1i,rtts=\"[357]\" 1563312120000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36275i,packet_size=572i,results=1i,rtts=\"[36275]\" 1563312120000000000")

    //val entries = oneRequest.flatMap(x => ICMPFactory.CreateICMPs(x))
    //entries.print

    val server = new SubscriptionServer
    server.run()

  }
}