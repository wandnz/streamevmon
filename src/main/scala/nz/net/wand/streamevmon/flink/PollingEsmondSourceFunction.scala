package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.connectors.esmond.schema.{EventType, Summary, TimeSeriesEntry}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.measurements.esmond.RichEsmondMeasurement

import java.time.{Duration, Instant}

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.util.{Failure, Try}

// TODO
// - We definitely want a way to filter out eventTypes we don't care about when
//   we discover. This could be done either via simple post-request filtering
//   here, or by allowing the user of this class to provide filtering query
//   strings for the discovery /archive/ call.
// - Configuration should be done properly. The constructor arguments are a
//   good starting point, but we'll probably need more than just that.
//   Particularly pay attention to what the stream discovery requires.
// - It might be worth a quick check to make sure the stream IDs for all the
//   outputs are actually unique.
// - I'm not sure if sequentially querying every endpoint with no delay is a
//   good way to approach initial startup. It might make more sense for this
//   to be more parallel, or to have more delay. It would also make sense to
//   skip that process when no fetchHistory is set, because that'll obviously
//   return nothing and it'll be more efficient to jump right into the regular
//   update loop.

class PollingEsmondSourceFunction(
  configPrefix             : String = "esmond.dataSource",
  fetchHistory             : Duration = Duration.ofMinutes(10),
  timeOffset               : Duration = Duration.ZERO,
  targetRefreshInterval    : Duration = Duration.ofMinutes(10),
  minimumTimeBetweenQueries: Duration = Duration.ofSeconds(1),
)
  extends RichSourceFunction[RichEsmondMeasurement]
          with GloballyStoppableFunction
          with Logging {
  @transient protected[this] var isRunning = false

  @transient protected[this] var esmond: Option[EsmondConnectionForeground] = None

  protected[this] var overrideParams: Option[ParameterTool] = None

  protected val firstMeasurementTime: Instant = Instant.now().minus(fetchHistory).minus(timeOffset)

  case class Endpoint(
    details: Either[EventType, Summary],
    lastMeasurementTime: Instant
  )

  protected[this] var selectedStreams: Option[Iterable[Endpoint]] = None

  def overrideConfig(config: ParameterTool): Unit = {
    overrideParams = Some(config)
  }

  protected def doSomethingWithMeasurement(timeSeriesEntry: TimeSeriesEntry): Unit = {
    logger.debug(timeSeriesEntry.toString)
  }

  protected def getSummaryEntries(
    summary  : Summary,
    timeStart: Option[Instant],
    timeEnd  : Option[Instant]
  ): Try[Iterable[TimeSeriesEntry]] = {
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
  ): Try[Iterable[TimeSeriesEntry]] = {
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
    // Set up config
    val params: ParameterTool = overrideParams.getOrElse(
      getRuntimeContext.getExecutionConfig.getGlobalJobParameters.asInstanceOf[ParameterTool]
    )
    esmond = Some(EsmondConnectionForeground(params.get(s"$configPrefix.serverName")))

    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this SourceFunction must be 1.")
    }

    // Figure out which streams we're going to use
    selectedStreams match {
      case Some(_) => // Do nothing, we already have a list of streams
      case None =>
        val discovery = new EsmondStreamDiscovery(
          configPrefix,
          esmond = esmond.get
        )
        selectedStreams = Some(
          discovery.discoverStreams().map(Endpoint(_, firstMeasurementTime))
        )
    }

    if (selectedStreams.get.isEmpty) {
      logger.error("Couldn't find any measurement streams!")
      return
    }
    selectedStreams = selectedStreams.map(streams => streams.take(2) ++ streams.filter(_.details.isRight).take(2))

    // Get historical data
    val now = Instant.now().minus(timeOffset)
    val timeSinceLastMeasurement = Duration.between(firstMeasurementTime, now)
    val historyString = DurationFormatUtils.formatDuration(timeSinceLastMeasurement.toMillis, "H:mm:ss")
    logger.info(s"Fetching data since ${firstMeasurementTime.plus(timeOffset)} ($historyString ago)")

    // Every stream should get queried, and the most recent measurement time updated.
    selectedStreams = selectedStreams.map(getAndUpdateEndpoints(ctx, _, Duration.ZERO))

    listen(ctx)
  }

  protected[this] def listen(ctx: SourceFunction.SourceContext[RichEsmondMeasurement]): Unit = {
    // targetRefreshInterval tells us how long we aim to have between each refresh
    // of a single endpoint. targetLoopInterval is based on that, and is our goal
    // for how long there should be between each query we send.
    // If it's smaller than minimumTimeBetweenQueries, we go with that instead.
    val loopInterval = {
      val targetLoopInterval = targetRefreshInterval.dividedBy(selectedStreams.get.size)
      if (targetLoopInterval.compareTo(minimumTimeBetweenQueries) < 0) {
        val durString = DurationFormatUtils.formatDuration(minimumTimeBetweenQueries.multipliedBy(selectedStreams.get.size).toMillis, "H:mm:ss")
        logger.warn(s"Too many endpoints to query. Time between queries will be longer than targetRefreshInterval (Approx $durString).")
        logger.warn("Reduce minimumTimeBetweenQueries, or number of queries to get more frequent updates. No data will be missed in either case.")
        minimumTimeBetweenQueries
      }
      else {
        targetLoopInterval
      }
    }
    val durationString = DurationFormatUtils.formatDuration(loopInterval.toMillis, "H:mm:ss")
    logger.info(s"Polling for events on ${selectedStreams.get.size} streams: One query every $durationString...")

    isRunning = true

    while (isRunning && !shouldShutdown) {
      selectedStreams = selectedStreams.map(getAndUpdateEndpoints(ctx, _, loopInterval))
    }
  }

  override def cancel(): Unit = {
    logger.info("Stopping esmond polling...")
    isRunning = false
  }
}
