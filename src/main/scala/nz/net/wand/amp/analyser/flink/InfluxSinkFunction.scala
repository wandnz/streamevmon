package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.Configuration
import nz.net.wand.amp.analyser.events.Event

import org.apache.flink.{configuration => flinkconf}
import org.apache.flink.configuration.IllegalConfigurationException
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.connectors.influxdb.{InfluxDBConfig, InfluxDBSink}
import org.influxdb.InfluxDBFactory

/** A [[https://ci.apache.org/projects/flink/flink-docs-stable/api/java/org/apache/flink/streaming/api/functions/sink/SinkFunction.html SinkFunction]]
  * which stores Event objects in InfluxDB.
  *
  * This class is a wrapper around InfluxDBSink provided by
  * [[https://github.com/apache/bahir-flink/tree/master/flink-connector-influxdb org.apache.bahir:flink-connector-influxdb]]
  * which allows us to sink our own [[nz.net.wand.amp.analyser.events.Event Events]] by utilising their
  * [[nz.net.wand.amp.analyser.events.Event.asInfluxPoint asInfluxPoint]] function.
  *
  * ==Configuration==
  *
  * This class is configured first by the `connectors.influx.sink` config key
  * group, then by `connectors.influx.dataSource` if a key is not found under
  * `sink`.
  *
  * - `serverName`: '''Required'''. The host that is running InfluxDB.
  *
  * - `portNumber`: The port that InfluxDB is running on.
  * Default 8086.
  *
  * - `user`: The username to use when connecting to InfluxDB.
  * Default "cuz".
  *
  * - `password`: The password to use when connecting to InfluxDB.
  * Default "".
  *
  * - `databaseName`: The name of the InfluxDB database to create or add to.
  * Default "analyser".
  *
  * @see [[https://ci.apache.org/projects/flink/flink-docs-stable/dev/table/sourceSinks.html]]
  * @see [[nz.net.wand.amp.analyser.connectors.InfluxConnection InfluxConnection]]
  */
class InfluxSinkFunction extends RichSinkFunction[Event] with Configuration {

  configPrefix = "connectors.influx"

  private[analyser] var url: String = {
    val address = getConfigString("sink.serverName").getOrElse {
      getConfigString("dataSource.serverName").getOrElse {
        throw new IllegalConfigurationException(
          s"You must specify $configPrefix.sink.serverName or $configPrefix.dataSource.serverName")
      }
    }

    val port = getConfigInt("sink.portNumber").getOrElse {
      getConfigInt("dataSource.portNumber").getOrElse(8086)
    }

    s"http://$address:$port"
  }

  private[analyser] var username: String = getConfigString("sink.user").getOrElse {
    getConfigString("dataSource.user").getOrElse("cuz")
  }

  private[analyser] var password: String = getConfigString("sink.password").getOrElse {
    getConfigString("dataSource.user").getOrElse("")
  }
  private[analyser] var database: String =
    getConfigString("sink.databaseName").getOrElse("analyser")

  private[this] lazy val sink: InfluxDBSink = new InfluxDBSink(
    InfluxDBConfig
      .builder(
        url,
        username,
        password,
        database
      )
      .build())

  private[this] var open: Boolean = false

  /** Initialisation method for RichFunctions. Occurs before any calls to `invoke()`.
    *
    * @param parameters Contains any configuration from program composition time.
    */
  override def open(parameters: flinkconf.Configuration): Unit = {
    val db = InfluxDBFactory.connect(url, username, password)

    if (!db.databaseExists(database)) {
      db.createDatabase(database)
    }

    sink.open(parameters)
    open = true
  }

  /** Teardown method for RichFunctions. Occurs after all calls to `invoke()`.
    */
  override def close(): Unit = {
    if (open && sink != null) {
      sink.close()
      open = false
    }
  }

  /** Called when new data arrives to the sink, and passes it to InfluxDB via bahir InfluxDBSink.
    *
    * @param value The data to send to InfluxDB.
    */
  override def invoke(value: Event): Unit = {
    sink.invoke(value.asInfluxPoint)
  }
}
