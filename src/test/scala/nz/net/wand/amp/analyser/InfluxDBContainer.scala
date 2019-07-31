package nz.net.wand.amp.analyser

import com.dimafeng.testcontainers.SingleContainer
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.testcontainers.containers.{InfluxDBContainer => OTCInfluxDBContainer}

import scala.language.existentials

class InfluxDBContainer(dockerImageNameOverride: Option[String] = None)
    extends SingleContainer[OTCInfluxDBContainer[_]] {

  type OTCContainer = OTCInfluxDBContainer[T] forSome { type T <: OTCInfluxDBContainer[T] }

  val username = "testUser"
  val password = "testPassword"
  val database = "testDatabase"
  val retentionPolicy = "testRP"

  override val container: OTCContainer = dockerImageNameOverride match {

    case Some(imageNameOverride) =>
      new OTCInfluxDBContainer(imageNameOverride)
        .withAdmin(username)
        .asInstanceOf[OTCContainer]
        .withAdminPassword(password)
        .asInstanceOf[OTCContainer]
        .withDatabase(database)
        .asInstanceOf[OTCContainer]

    case None =>
      new OTCInfluxDBContainer()
  }

  def credentials = InfluxCredentials(username, password)

  def address: String = container.getContainerIpAddress

  def port: Int = {
    container.getMappedPort(OTCInfluxDBContainer.INFLUXDB_PORT)
  }
}

object InfluxDBContainer {

  def apply(dockerImageNameOverride: String = null): InfluxDBContainer =
    new InfluxDBContainer(Option(dockerImageNameOverride))
}
