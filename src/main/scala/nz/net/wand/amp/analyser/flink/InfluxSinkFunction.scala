package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.{Configuration, Logging}
import nz.net.wand.amp.analyser.events.Event

import org.apache.flink.{configuration => flinkconf}
import org.apache.flink.configuration.IllegalConfigurationException
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.connectors.influxdb.{InfluxDBConfig, InfluxDBSink}
import org.influxdb.InfluxDBFactory

class InfluxSinkFunction extends RichSinkFunction[Event] with Configuration with Logging {

  configPrefix = "connectors.influx"

  var url: String = {
    val address = getConfigString("dataSource.serverName").getOrElse {
      getConfigString("sink.serverName").getOrElse {
        throw new IllegalConfigurationException(
          s"You must specify $configPrefix.dataSource.serverName or $configPrefix.sink.serverName")
      }
    }

    val port = getConfigInt("dataSource.portNumber").getOrElse {
      getConfigInt("sink.portNumber").getOrElse(8086)
    }

    s"http://$address:$port"
  }

  var username: String = getConfigString("dataSource.user").getOrElse {
    getConfigString("sink.user").getOrElse("cuz")
  }

  var password: String = getConfigString("dataSource.password").getOrElse {
    getConfigString("sink.user").getOrElse("cuz")
  }
  var database: String = getConfigString("sink.databaseName").getOrElse("analyser")

  private[this] lazy val sink: InfluxDBSink = new InfluxDBSink(
    InfluxDBConfig
      .builder(
        url,
        username,
        password,
        database
      )
      .build())

  var open: Boolean = false

  override def open(parameters: flinkconf.Configuration): Unit = {
    val db = InfluxDBFactory.connect(url, username, password)

    if (!db.databaseExists(database)) {
      db.createDatabase(database)
    }

    sink.open(parameters)
    open = true
  }

  override def close(): Unit = {
    if (open && sink != null) {
      sink.close()
      open = false
    }
  }

  override def invoke(value: Event): Unit = {
    sink.invoke(value.asInfluxPoint)
  }
}
