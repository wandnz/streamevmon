package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.esmond.schema.{AbstractTimeSeriesEntry, EventType, Summary}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond._
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.esmond.RichEsmondMeasurement

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

/** Outputs data from an Esmond API host.
  *
  * Discovers the streams to query by passing the configuration to an instance
  * of [[nz.net.wand.streamevmon.connectors.esmond.AbstractEsmondStreamDiscovery AbstractEsmondStreamDiscovery]]
  * constructed using `discoveryBuilder`.
  *
  * Configuration for both can be overridden by using `overrideConfig`.
  *
  * @param configPrefix      A custom config prefix to use. You will need one instance
  *                          of this SourceFunction for each endpoint being queried,
  *                          and each will need a unique configPrefix.
  * @param connectionBuilder A function which builds the Esmond connection
  *                          implementation.
  */
class PollingEsmondSourceFunction[
  EsmondConnectionT <: AbstractEsmondConnection,
  EsmondDiscoveryT <: AbstractEsmondStreamDiscovery
](
  configPrefix     : String = "source.esmond",
  // Man, this is a pain. You can't instantiate a type parameter, so instead
  // you've got to include builders as arguments. These defaults are sensible.
  // Mainly useful for testing.
  connectionBuilder: String => AbstractEsmondConnection =
  (s: String) => new EsmondConnectionForeground(s),
  discoveryBuilder : (String, ParameterTool, AbstractEsmondConnection) => AbstractEsmondStreamDiscovery =
  (s: String, p: ParameterTool, c: AbstractEsmondConnection) => new EsmondStreamDiscovery(s, p, c)
)
  extends RichSourceFunction[RichEsmondMeasurement]
          with HasFlinkConfig
          with CheckpointedFunction
          with GloballyStoppableFunction
          with Logging {

  /** Keeps track of an endpoint that's being queried, as well as the time of
    * the last measurement we received. This lets us only query for measurements
    * we haven't seen before.
    */
  protected case class Endpoint(
    details            : Either[EventType, Summary],
    lastMeasurementTime: Instant
  )

  @transient protected var isRunning = false

  @transient protected var esmond: Option[AbstractEsmondConnection] = None

  protected var firstMeasurementTime: Instant = _

  @transient protected var fetchHistory: Duration = _

  @transient protected var timeOffset: Duration = _

  @transient protected var targetRefreshInterval: Duration = _

  @transient protected var minimumTimeBetweenQueries: Duration = _

  protected var selectedStreams: Iterable[Endpoint] = Seq()
  @transient protected var selectedStreamsStorage: ListState[Endpoint] = _

  override val configKeyGroup: String = configPrefix
  override val flinkName: String = "Polling Esmond Source"
  override val flinkUid: String = "polling-flink-source"

  override def open(parameters: Configuration): Unit = {
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Set up config
    val params: ParameterTool = configWithOverride(getRuntimeContext)
    esmond = Some(connectionBuilder(params.get(s"$configPrefix.serverName")))

    fetchHistory = Duration.ofSeconds(params.getInt(s"$configPrefix.fetchHistory"))
    timeOffset = Duration.ofSeconds(params.getInt(s"$configPrefix.timeOffset"))
    targetRefreshInterval = Duration.ofSeconds(params.getInt(s"$configPrefix.targetRefreshInterval"))
    minimumTimeBetweenQueries = Duration.ofSeconds(params.getInt(s"$configPrefix.minimumTimeBetweenQueries"))

    firstMeasurementTime = Instant.now().minus(fetchHistory).minus(timeOffset)
  }

  // Wrapper functions to scream and yell if the esmond connection doesn't exist
  protected def getSummaryEntries(
    summary: Summary,
    timeStart: Option[Instant],
    timeEnd  : Option[Instant]
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    esmond match {
      case Some(es) =>
        es.getTimeSeriesSummaryEntries(
          summary,
          timeStart = timeStart.map(_.getEpochSecond),
          timeEnd = timeEnd.map(_.getEpochSecond)
        )
      case None => Failure(new IllegalStateException("Can't get entries without a valid esmond connection."))
    }
  }

  protected def getEntries(
    eventType: EventType,
    timeStart: Option[Instant],
    timeEnd  : Option[Instant]
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    esmond match {
      case Some(es) =>
        es.getTimeSeriesEntries(
          eventType,
          timeStart = timeStart.map(_.getEpochSecond),
          timeEnd = timeEnd.map(_.getEpochSecond)
        )
      case None => Failure(new IllegalStateException("Can't get entries without a valid esmond connection."))
    }
  }

  /** This function does all the heavy lifting of querying the endpoints and
    * waiting the appropriate amount of time between queries.
    *
    * @param endpoints    The list of endpoints which should be queried, as well
    *                     as the last observed time from that endpoint.
    * @param loopInterval The amount of time to wait between queries.
    *
    * @return `endpoints`, updated with the last observed times from any
    *         endpoints which returned data.
    */
  protected def getAndUpdateEndpoints(
    ctx         : SourceFunction.SourceContext[RichEsmondMeasurement],
    endpoints   : Iterable[Endpoint],
    loopInterval: Duration
  ): Iterable[Endpoint] = {
    // The first query happens instantly. This lets us have 0-delay queries,
    // but does mean that if this function is called in a loop with no delay,
    // the last and first queries will happen with no delay.
    var lastQueryTime: Instant = Instant.now().minus(loopInterval)

    // For every endpoint
    endpoints.map { endpoint =>
      // If we need to wait around to not bog down the API too much, then do so.
      val now = Instant.now()
      val targetQueryTime = lastQueryTime.plus(loopInterval)
      if (now.compareTo(targetQueryTime) < 0) {
        val timeToSleep = targetQueryTime.toEpochMilli - now.toEpochMilli
        Thread.sleep(timeToSleep)
      }
      lastQueryTime = Instant.now()

      // Pick the function to use depending on if it's a summary or not.
      endpoint.details match {
        case Left(eType) =>
          // Get the entries since however long ago we're querying
          val entries = getEntries(
            eType,
            timeStart = Some(endpoint.lastMeasurementTime),
            timeEnd = Some(lastQueryTime.minus(timeOffset))
          )
          // Submit them to Flink
          entries.foreach(_.foreach(entry => ctx.collect(RichEsmondMeasurement(eType, entry))))
          // And update our list of entries.
          // If we got any entries, we should update the time of the last seen measurement.
          if (entries.isSuccess && entries.get.nonEmpty) {
            Endpoint(
              endpoint.details,
              Instant.ofEpochSecond(entries.get.maxBy(_.timestamp).timestamp).plusSeconds(1)
            )
          }
          // Otherwise, just keep the time the same. The details remain intact either way.
          else {
            endpoint
          }
        case Right(summary) =>
          val entries = getSummaryEntries(
            summary,
            timeStart = Some(endpoint.lastMeasurementTime),
            timeEnd = Some(lastQueryTime.minus(timeOffset))
          )
          entries.foreach(_.foreach(entry => ctx.collect(RichEsmondMeasurement(summary, entry))))
          if (entries.isSuccess && entries.get.nonEmpty) {
            Endpoint(
              endpoint.details,
              Instant.ofEpochSecond(entries.get.maxBy(_.timestamp).timestamp).plusSeconds(1)
            )
          }
          else {
            endpoint
          }
      }
    }
  }

  override def run(ctx: SourceFunction.SourceContext[RichEsmondMeasurement]): Unit = {
    // Figure out which streams we're going to use
    if (selectedStreams.isEmpty) {
      val discovery = discoveryBuilder(
        configPrefix,
        overrideParams.getOrElse(
          getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
        ),
        esmond.get
      )
      selectedStreams = discovery.discoverStreams().map(Endpoint(_, firstMeasurementTime))
    }

    if (selectedStreams.isEmpty) {
      logger.error("Couldn't find any measurement streams!")
      return
    }

    // This takes just two of each kind of stream for debugging purposes.
    //selectedStreams = selectedStreams.take(2) ++ selectedStreams.filter(_.details.isRight).take(2)

    // Get historical data
    val now = Instant.now().minus(timeOffset)
    val timeSinceLastMeasurement = Duration.between(firstMeasurementTime, now)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since ${firstMeasurementTime.plus(timeOffset)} ($historyString ago)")

    // Every stream gets, and the most recent measurement time updated.
    selectedStreams = getAndUpdateEndpoints(ctx, selectedStreams, Duration.ZERO)

    // Then just keep on polling until we're told to quit.
    listen(ctx)
  }

  /** Continually polls all the endpoints for new data. */
  protected[this] def listen(ctx: SourceFunction.SourceContext[RichEsmondMeasurement]): Unit = {
    // targetRefreshInterval tells us how long we aim to have between each refresh
    // of a single endpoint. targetLoopInterval is based on that, and is our goal
    // for how long there should be between each query we send.
    // If it's smaller than minimumTimeBetweenQueries, we go with that instead.
    val loopInterval = {
      val targetLoopInterval = targetRefreshInterval.dividedBy(selectedStreams.size)
      if (targetLoopInterval.compareTo(minimumTimeBetweenQueries) < 0) {
        val durString = DurationFormatUtils.formatDuration(minimumTimeBetweenQueries.multipliedBy(selectedStreams.size).toMillis, "H:mm:ss")
        logger.warn(s"Too many endpoints to query. Time between queries will be longer than targetRefreshInterval (Approx $durString).")
        logger.warn("Reduce minimumTimeBetweenQueries, or number of queries to get more frequent updates. No data will be missed in either case.")
        minimumTimeBetweenQueries
      }
      else {
        targetLoopInterval
      }
    }
    val durationString = DurationFormatUtils.formatDuration(loopInterval.toMillis, "H:mm:ss")
    logger.info(s"Polling for events on ${selectedStreams.size} streams: One query every $durationString...")

    isRunning = true

    while (isRunning && !shouldShutdown) {
      selectedStreams = getAndUpdateEndpoints(ctx, selectedStreams, loopInterval)
    }
  }

  override def cancel(): Unit = {
    logger.info("Stopping esmond polling...")
    isRunning = false
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    selectedStreamsStorage.clear()
    selectedStreamsStorage.addAll(selectedStreams.toList.asJava)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val descriptor = new ListStateDescriptor[Endpoint](
      "Selected Streams",
      TypeInformation.of(classOf[Endpoint])
    )
    selectedStreamsStorage = context.getOperatorStateStore.getListState(descriptor)
    if (context.isRestored) {
      selectedStreams = selectedStreamsStorage.get().asScala
    }
  }
}
