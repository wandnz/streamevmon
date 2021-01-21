package nz.net.wand.streamevmon.flink.sinks

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.flink.HasFlinkConfig

import com.github.fsanaulla.chronicler.ahc.io.AhcIOClient
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.{configuration => flinkconf}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/** A SinkFunction which stores Event objects in InfluxDB. This function makes
  * use of Flink checkpointing to ensure that no Events get lost in a failure.
  *
  * ==Configuration==
  *
  * This class can be configured by several config key groups. When looking for
  * most keys, it will first search under `influx.sink`. If it's not found there,
  * it will look under `influx.source`, then `influx.source.amp`. ''Note that
  * this means the InfluxDB username and password will default to the
  * credentials for an AMP Influx instance if not specified!''
  *
  * This behaviour does not apply to `databaseName` or `retentionPolicy`.
  *
  * - `serverName`: The host that is running InfluxDB.
  * Default localhost.
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
  * @see [[nz.net.wand.streamevmon.connectors.influx Influx connectors package]]
  */
class InfluxSinkFunction
  extends RichSinkFunction[Event]
          with HasFlinkConfig
          with CheckpointedFunction
          with Logging {

  override val flinkName: String = "Influx Sink"
  override val flinkUid: String = "influx-sink"
  override val configKeyGroup: String = "influx"

  private var host: String = _

  private var port: Int = _

  private var username: String = _

  private var password: String = _

  private var database: String = _

  private var retentionPolicy: String = _

  private var influx: AhcIOClient = _

  private val bufferedEvents: mutable.Buffer[Event] = mutable.Buffer()

  private def getWithFallback(parameters: ParameterTool, key: String): String = {
    var result = parameters.get(s"sink.$configKeyGroup.$key", null)
    if (result == null) {
      result = parameters.get(s"source.$configKeyGroup.$key", null)
      if (result == null) {
        parameters.get(s"source.$configKeyGroup.amp.$key")
      }
    }
    result
  }

  private def getHost(p: ParameterTool): String = {
    val host = Try(getWithFallback(p, "serverName"))
    host match {
      case Failure(_) => throw new RuntimeException(
        "You must specify the config key 'source.influx.serverName' " +
          "or 'sink.influx.serverName'.")
      case Success(value) => value
    }
  }

  /** Initialisation method for RichFunctions. Occurs before any calls to `invoke()`.
    *
    * @param parameters Ignored.
    */
  override def open(parameters: flinkconf.Configuration): Unit = {
    val p = configWithOverride(getRuntimeContext)

    host = getHost(p)
    port = getWithFallback(p, "portNumber").toInt
    username = getWithFallback(p, "user")
    password = getWithFallback(p, "password")
    database = p.get(s"sink.$configKeyGroup.databaseName")
    retentionPolicy = p.get(s"sink.$configKeyGroup.retentionPolicy")

    influx = new AhcIOClient(
      host,
      port,
      false,
      Some(InfluxCredentials(username, password)),
      None
    )

    val mng = InfluxMng(host, port, Some(InfluxCredentials(username, password)))

    mng.createDatabase(database)
    mng.createRetentionPolicy(retentionPolicy, database, "0s", default = true)
  }

  /** Teardown method for RichFunctions. Occurs after all calls to `invoke()`. */
  override def close(): Unit = {
    if (influx != null) {
      influx.close()
    }
  }

  /** Called when new data arrives to the sink, and passes it to InfluxDB.
    *
    * @param value The data to send to InfluxDB.
    */
  override def invoke(value: Event, context: Context): Unit = {
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

  private var checkpointState: ListState[Event] = _

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointState.clear()
    checkpointState.addAll(bufferedEvents.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    checkpointState = context
      .getOperatorStateStore
      .getListState(new ListStateDescriptor[Event]("bufferedEvents", classOf[Event]))

    if (context.isRestored) {
      checkpointState.get().forEach { item => bufferedEvents.append(item) }
    }
  }
}
