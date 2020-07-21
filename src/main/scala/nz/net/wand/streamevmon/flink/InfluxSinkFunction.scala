package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.detectors.HasFlinkConfig

import java.util.{List => JList}

import com.github.fsanaulla.chronicler.ahc.io.AhcIOClient
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.{configuration => flinkconf}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/** A [[https://ci.apache.org/projects/flink/flink-docs-stable/api/java/org/apache/flink/streaming/api/functions/sink/SinkFunction.html SinkFunction]]
  * which stores Event objects in InfluxDB.
  *
  * ==Configuration==
  *
  * This class is configured first by the `influx.sink` config key group, then
  * by `influx.source` and finally `influx.source.amp` if a key
  * is not found under `sink`. ''Note that this means the InfluxDB username and
  * password will default to the credentials for an AMP Influx instance if not
  * specified!''
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
  * Default "streamevmon".
  *
  * - `retentionPolicy`: The name of the retention policy to create or add to.
  * Default "streamevmondefault".
  *
  * @see [[https://ci.apache.org/projects/flink/flink-docs-stable/dev/table/sourceSinks.html]]
  * @see [[nz.net.wand.streamevmon.connectors.InfluxConnection InfluxConnection]]
  */
class InfluxSinkFunction
  extends RichSinkFunction[Event]
          with HasFlinkConfig
          with ListCheckpointed[Event]
          with Logging {

  private[streamevmon] var host: String = _

  private[streamevmon] var port: Int = _

  private[streamevmon] var username: String = _

  private[streamevmon] var password: String = _

  private[streamevmon] var database: String = _

  private[streamevmon] var retentionPolicy: String = _

  override val flinkName: String = "Influx Sink"
  override val flinkUid: String = "influx-sink"
  override val configKeyGroup: String = "sink.influx"

  private[this] var influx: AhcIOClient = _

  private[this] val bufferedEvents: ListBuffer[Event] = ListBuffer()

  /** A pretty gross way of getting a key from the config structure, with
    * preference for influx.sink and then source.influx.
    */
  private[this] def getWithFallback(parameters: ParameterTool, key: String): String = {
    var result = parameters.get(s"sink.influx.$key", null)
    if (result == null) {
      result = parameters.get(s"source.influx.$key", null)
      if (result == null) {
        parameters.get(s"source.influx.amp.$key", null)
      }
      else {
        result
      }
    }
    else {
      result
    }
  }

  private[this] def getHost(p: ParameterTool): String = {
    val host = getWithFallback(p, "serverName")
    if (host == null) {
      throw new RuntimeException(
        "You must specify the config key 'source.influx.serverName' " +
          "or 'sink.influx.serverName'.")
    }
    host
  }

  /** Initialisation method for RichFunctions. Occurs before any calls to `invoke()`.
    *
    * @param parameters Ignored.
    */
  override def open(parameters: flinkconf.Configuration): Unit = {
    val p = if (overrideParams.isDefined) {
      overrideParams.get
    }
    else {
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    }

    host = getHost(p)
    port = getWithFallback(p, "portNumber").toInt
    username = getWithFallback(p, "user")
    password = getWithFallback(p, "password")
    database = p.get("influx.sink.databaseName")
    retentionPolicy = p.get("influx.sink.retentionPolicy")

    influx = new AhcIOClient(
      host,
      port,
      false,
      Some(InfluxCredentials(username, password)),
      None
    )

    val mng = InfluxMng(host, port, Some(InfluxCredentials(username, password)))

    mng.createDatabase(database)
    mng.createRetentionPolicy(retentionPolicy, database, "8760h0m0s", default = true)
  }

  /** Teardown method for RichFunctions. Occurs after all calls to `invoke()`.
    */
  override def close(): Unit = {
    if (influx != null) {
      influx.close()
    }
  }

  /** Called when new data arrives to the sink, and passes it to InfluxDB.
    *
    * @param value The data to send to InfluxDB.
    */
  override def invoke(value: Event, context: Context[_]): Unit = {
    bufferedEvents.append(value)
    val meas = influx.measurement[Event](database, value.eventType)

    var success = false
    while (!success) {
      Await.result(
        meas.write(value, retentionPolicy = Some(retentionPolicy))(Event.getWriter).flatMap {
          case Left(err) => Future {
            logger.error(s"Failed to write to InfluxDB: $err")
            Thread.sleep(500)
          }
          case Right(_) => Future {
            success = true
            bufferedEvents.remove(bufferedEvents.indexOf(value))
          }
        },
        Duration.Inf
      )
    }
  }

  override def snapshotState(checkpointId: Long, timestamp: Long): JList[Event] = {
    bufferedEvents.asJava
  }

  override def restoreState(state: JList[Event]): Unit = {
    bufferedEvents ++= state.asScala
  }
}
