package nz.net.wand.amp.analyser.connectors

import nz.net.wand.amp.analyser.InfluxDBContainer
import nz.net.wand.amp.analyser.flink.InfluxSinkFunction

import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import org.scalatest.WordSpec

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

    InfluxConnection.influx = Some(influx)
    InfluxConnection.dbName = container.database
    InfluxConnection.rpName = container.retentionPolicy
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
