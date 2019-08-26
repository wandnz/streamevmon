package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.events.Event

import org.apache.flink.{configuration => flinkconf}
import org.apache.flink.api.java.utils.ParameterTool
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
  * This class is configured first by the `influx.sink` config key group, then
  * by `influx.dataSource` if a key is not found under `sink`.
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
  * The data is stored under the "autogen" retention policy. Unfortunately, this
  * cannot be changed.
  *
  * @see [[https://ci.apache.org/projects/flink/flink-docs-stable/dev/table/sourceSinks.html]]
  * @see [[nz.net.wand.amp.analyser.connectors.InfluxConnection InfluxConnection]]
  */
class InfluxSinkFunction extends RichSinkFunction[Event] {

  private[analyser] var url: String = _

  private[analyser] var username: String = _

  private[analyser] var password: String = _

  private[analyser] var database: String = _

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

  private[this] def getWithFallback(parameters: ParameterTool, key: String): String = {
    val result = parameters.get(s"influx.sink.$key", "unset-sink-config-option")
    if (result == "unset-sink-config-option") {
      parameters.get(s"influx.dataSource.$key", null)
    }
    else {
      result
    }
  }

  private[this] def makeUrl(parameters: ParameterTool): String = {
    val address = {
      val sinkName = getWithFallback(parameters, "serverName")
      if (sinkName == null) {
        throw new RuntimeException(
          "You must specify the config key 'influx.dataSource.serverName' " +
            "or 'influx.sink.serverName'.")
      }
      sinkName
    }

    val port = {
      getWithFallback(parameters, "portNumber").toInt
    }

    s"http://$address:$port"
  }

  /** Initialisation method for RichFunctions. Occurs before any calls to `invoke()`.
    *
    * @param parameters Contains any configuration from program composition time.
    */
  override def open(parameters: flinkconf.Configuration): Unit = {
    val p = getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    url = makeUrl(p)
    username = getWithFallback(p, "user")
    password = getWithFallback(p, "password")
    database = p.get("influx.sink.databaseName", null)

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
