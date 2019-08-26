package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.flink.InfluxSinkFunction

import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import org.apache.flink.api.java.utils.ParameterTool
import org.scalatest.WordSpec

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxContainerSpec extends WordSpec with ForAllTestContainer {
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

  protected def getInflux(subscriptionName: String, listenAddress: String = null): InfluxConnection = {
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

  protected def getInfluxConfigMap(subscriptionName: String, listenAddress: String = null): Map[String, String] = {
    Map(
      "influx.dataSource.subscriptionName" -> subscriptionName,
      "influx.dataSource.databaseName" -> container.database,
      "influx.dataSource.retentionPolicyName" -> container.retentionPolicy,
      "influx.dataSource.listenProtocol" -> "http",
      "influx.dataSource.listenAddress" -> listenAddress,
      "influx.dataSource.listenPort" -> "0",
      "influx.dataSource.listenBacklog" -> "5",
      "influx.dataSource.serverName" -> container.address,
      "influx.dataSource.portNumber" -> container.port.toString,
      "influx.dataSource.user" -> container.username,
      "influx.dataSource.password" -> container.password,
      "influx.sink.databaseName" -> container.database,
      "flink.maxLateness" -> "1"
    )
  }

  protected def getInfluxConfig(subscriptionName: String, listenAddress: String = null): ParameterTool = {
    ParameterTool.fromMap(getInfluxConfigMap(subscriptionName, listenAddress).asJava)
  }

  protected def getSinkFunction: InfluxSinkFunction = {
    val sink = new InfluxSinkFunction

    sink.url = s"http://${container.address}:${container.port}"
    sink.username = container.username
    sink.password = container.password
    sink.database = container.database

    sink
  }
}
