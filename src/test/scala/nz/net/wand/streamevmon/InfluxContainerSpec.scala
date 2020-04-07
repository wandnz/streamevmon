package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.flink.InfluxSinkFunction

import com.dimafeng.testcontainers.{ForAllTestContainer, InfluxDBContainer => DIDBContainer}
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxContainerSpec extends TestBase with ForAllTestContainer {

  // Influx containers have been a little flaky in the past, and official
  // support from testcontainers-scala is fairly new, so we'll stick to their
  // default version for now just to make sure. We will use the alpine image
  // for the size benefits.
  // We're turning off auth because we don't really care.
  // See [[PostgresContainerSpec]] for a little more detail.
  override val container: DIDBContainer = DIDBContainer(tag = s"${DIDBContainer.defaultTag}-alpine", authEnabled = false)

  protected lazy val containerAddress = container.containerIpAddress
  protected lazy val containerPort = container.mappedPort(8086)

  override def afterStart(): Unit = {
    val influx = InfluxMng(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

    Await.result(influx.updateRetentionPolicy(
      "autogen",
      container.database,
      duration = Some("8760h0m0s")
    ),
      Duration.Inf)
  }

  protected def getInfluxSubscriber(
    subscriptionName: String,
    listenAddress: String = null
  ): InfluxConnection = {
    InfluxConnection(
      subscriptionName,
      container.database,
      "autogen",
      "http",
      if (listenAddress == null) {
        InfluxConnection.getListenAddress
      }
      else {
        listenAddress
      },
      0,
      5,
      containerAddress,
      containerPort,
      container.username,
      container.password
    )
  }

  protected def getInfluxHistory: InfluxHistoryConnection = {
    InfluxHistoryConnection(
      container.database,
      "autogen",
      containerAddress,
      containerPort,
      container.username,
      container.password
    )
  }

  /** @param listenAddress This defaults to null so that the ParameterTool
    *                      won't set the key.
    */
  protected def getInfluxConfigMap(
    subscriptionName: String,
    listenAddress: String = null
  ): Map[String, String] = {
    Map(
      "influx.dataSource.default.subscriptionName" -> subscriptionName,
      "influx.dataSource.default.databaseName" -> container.database,
      "influx.dataSource.default.retentionPolicyName" -> "autogen",
      "influx.dataSource.default.listenProtocol" -> "http",
      "influx.dataSource.default.listenAddress" -> listenAddress,
      "influx.dataSource.default.listenPort" -> "0",
      "influx.dataSource.default.listenBacklog" -> "5",
      "influx.dataSource.default.serverName" -> containerAddress,
      "influx.dataSource.default.portNumber" -> containerPort.toString,
      "influx.dataSource.default.user" -> container.username,
      "influx.dataSource.default.password" -> container.password,
      "influx.sink.databaseName" -> container.database,
      "influx.sink.retentionPolicy" -> "autogen",
      "flink.maxLateness" -> "1"
    )
  }

  protected def getInfluxConfig(
    subscriptionName: String,
    listenAddress: String = null
  ): ParameterTool = {
    ParameterTool.fromMap(getInfluxConfigMap(subscriptionName, listenAddress).asJava)
  }

  protected def getSinkFunction: InfluxSinkFunction = {
    val sink = new InfluxSinkFunction

    sink.host = containerAddress
    sink.port = containerPort
    sink.username = container.username
    sink.password = container.password
    sink.database = container.database

    sink
  }
}
