package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.flink.InfluxSinkFunction

import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxContainerSpec extends TestBase with ForAllTestContainer {
  override val container: InfluxDBContainer = InfluxDBContainer("alpine")

  override def afterStart(): Unit = {
    val influx =
      InfluxMng(container.address, container.port, Some(container.credentials))

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
      container.retentionPolicy,
      "http",
      if (listenAddress == null) {
        InfluxConnection.getListenAddress
      }
      else {
        listenAddress
      },
      0,
      5,
      container.address,
      container.port,
      container.username,
      container.password
    )
  }

  protected def getInfluxHistory: InfluxHistoryConnection = {
    InfluxHistoryConnection(
      container.database,
      container.retentionPolicy,
      container.address,
      container.port,
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
      "influx.dataSource.default.retentionPolicyName" -> container.retentionPolicy,
      "influx.dataSource.default.listenProtocol" -> "http",
      "influx.dataSource.default.listenAddress" -> listenAddress,
      "influx.dataSource.default.listenPort" -> "0",
      "influx.dataSource.default.listenBacklog" -> "5",
      "influx.dataSource.default.serverName" -> container.address,
      "influx.dataSource.default.portNumber" -> container.port.toString,
      "influx.dataSource.default.user" -> container.username,
      "influx.dataSource.default.password" -> container.password,
      "influx.sink.databaseName" -> container.database,
      "influx.sink.retentionPolicy" -> container.retentionPolicy,
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

    sink.host = container.address
    sink.port = container.port
    sink.username = container.username
    sink.password = container.password
    sink.database = container.database

    sink
  }
}
