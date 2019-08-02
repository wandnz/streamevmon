package nz.net.wand.amp.analyser.connectors

import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import org.scalatest.WordSpec

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxContainerTest extends WordSpec with ForAllTestContainer {
  override val container: InfluxDBContainer = InfluxDBContainer("alpine")

  override def afterStart(): Unit = {
    val influx =
      InfluxMng(container.address, container.port, Some(container.credentials))

    Await.result(influx.createRetentionPolicy(
                   container.retentionPolicy,
                   container.database,
                   "8760h0m0s",
                   default = true
                 ),
                 Duration.Inf)

    InfluxConnection.influx = Some(influx)
    InfluxConnection.dbName = container.database
    InfluxConnection.rpName = container.retentionPolicy
  }
}
