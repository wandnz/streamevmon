package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.esmond.schema.{AbstractTimeSeriesEntry, Summary}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond._
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

class PollingEsmondSourceFunction[
  EsmondConnectionT <: AbstractEsmondConnection,
  EsmondDiscoveryT <: AbstractEsmondStreamDiscovery
](
  configPrefix     : String = "esmond.dataSource",
  // Man, this is a pain. You can't instantiate a type parameter, so instead
  // you've got to include builders as arguments. These defaults are sensible,
  // but I wanted to do this anyway so I could mock connections during testing.
  connectionBuilder: String => AbstractEsmondConnection =
  (s: String) => new EsmondConnectionForeground(s),
  discoveryBuilder : (String, ParameterTool, AbstractEsmondConnection) => AbstractEsmondStreamDiscovery =
  (s: String, p: ParameterTool, c: AbstractEsmondConnection) => new EsmondStreamDiscovery(s, p, c)
)
  extends RichSourceFunction[RichEsmondMeasurement]
          with CheckpointedFunction
          with GloballyStoppableFunction
          with Logging {

  @transient protected var isRunning = false

  @transient protected var esmond: Option[AbstractEsmondConnection] = None

  protected var overrideParams: Option[ParameterTool] = None

  protected var firstMeasurementTime: Instant = _

  @transient protected var fetchHistory: Duration = _

  @transient protected var timeOffset: Duration = _

  @transient protected var targetRefreshInterval: Duration = _

  @transient protected var minimumTimeBetweenQueries: Duration = _

  protected var selectedStreams: Iterable[Endpoint] = Seq()
  @transient protected var selectedStreamsStorage: ListState[Endpoint] = _

  def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
  }

  override def open(parameters: Configuration): Unit = {
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Set up config
    val params: ParameterTool = overrideParams.getOrElse(
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    )
    esmond = Some(connectionBuilder(params.get(s"$configPrefix.serverName")))

    fetchHistory = Duration.ofSeconds(params.getInt(s"$configPrefix.fetchHistory"))
    timeOffset = Duration.ofSeconds(params.getInt(s"$configPrefix.timeOffset"))
    targetRefreshInterval = Duration.ofSeconds(params.getInt(s"$configPrefix.targetRefreshInterval"))
    minimumTimeBetweenQueries = Duration.ofSeconds(params.getInt(s"$configPrefix.minimumTimeBetweenQueries"))

    firstMeasurementTime = Instant.now().minus(fetchHistory).minus(timeOffset)
  }

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
    metadataKey: String,
    eventType  : String,
    timeStart  : Option[Instant],
    timeEnd    : Option[Instant]
  ): Try[Iterable[AbstractTimeSeriesEntry]] = {
    esmond match {
      case Some(es) =>
        es.getTimeSeriesEntries(
          metadataKey,
          eventType,
          timeStart = timeStart.map(_.getEpochSecond),
          timeEnd = timeEnd.map(_.getEpochSecond)
        )
      case None => Failure(new IllegalStateException("Can't get entries without a valid esmond connection."))
    }
  }

  protected def getAndUpdateEndpoints(
    ctx: SourceFunction.SourceContext[RichEsmondMeasurement],
    endpoints: Iterable[Endpoint],
    loopInterval: Duration
  ): Iterable[Endpoint] = {
    var lastQueryTime: Instant = Instant.now().minus(loopInterval)

    // depending on if they're summaries or not...
    endpoints.map { endpoint =>
      // If we need to wait around to not bog down the API too much, then do so.
      val now = Instant.now()
      val targetQueryTime = lastQueryTime.plus(loopInterval)
      if (now.compareTo(targetQueryTime) < 0) {
        val timeToSleep = targetQueryTime.toEpochMilli - now.toEpochMilli
        Thread.sleep(timeToSleep)
      }
      lastQueryTime = Instant.now()

      endpoint.details match {
        case Left(eType) =>
          // Get the entries since however long ago we're querying
          val entries = getEntries(
            eType.metadataKey,
            eType.eventType,
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
    selectedStreams = selectedStreams.take(2) ++ selectedStreams.filter(_.details.isRight).take(2)

    // Get historical data
    val now = Instant.now().minus(timeOffset)
    val timeSinceLastMeasurement = Duration.between(firstMeasurementTime, now)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since ${firstMeasurementTime.plus(timeOffset)} ($historyString ago)")

    // Every stream should get queried, and the most recent measurement time updated.
    selectedStreams = getAndUpdateEndpoints(ctx, selectedStreams, Duration.ZERO)

    listen(ctx)
  }

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
