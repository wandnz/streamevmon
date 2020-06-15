package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.connectors.esmond.EsmondConnectionForeground
import nz.net.wand.streamevmon.connectors.esmond.schema.Archive
import nz.net.wand.streamevmon.flink.PollingEsmondSourceFunction

import java.io._
import java.time.{Duration, Instant}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

import scala.util.{Failure, Success, Try}

object EsmondRunner extends Logging {
  val timeRange: Long = 86400
  val eventType = "packet-count-sent"

  private val connection = new EsmondConnectionForeground("http://denv-owamp.es.net:8085")

  /** This function tries out all the API calls and does some basic sanity checking. */
  def useAllTheCalls(): Unit = {
    // Let's get the full list of supported time series within the relevant timeRange.
    val archiveFull = connection.getArchiveList(timeRange = Some(timeRange))

    // If it crashed out, then we can't continue.
    if (archiveFull.isFailure) {
      logger.error(s"Exception getting archive list: ${archiveFull.failed.get}")
      return
    }

    logger.info(s"Got ${archiveFull.get.size} entries in archive")
    logger.info(s"Full unbound list is ${connection.getArchiveList().get.size} entries")

    // We'll select the first entry in the archive that contains the kind of event type we want.
    // This is just a demonstration, after all.
    val selectedArchive = archiveFull.get.find(_.eventTypes.exists(_.eventType == eventType))
    // If there wasn't any, we'll give up.
    if (selectedArchive.isEmpty) {
      logger.error(s"No entries of event type $eventType were found.")
      return
    }
    // We'll also get a reference to the event listing of the type we want.
    val selectedEvent = selectedArchive.get.eventTypes.find(_.eventType == eventType)

    val selectedArchiveMirror = connection.getArchive(selectedArchive.get.metadataKey)

    if (selectedArchiveMirror.get != selectedArchive.get) {
      logger.warn(s"Archive from listing and from direct access are different!")
    }

    // Now that we know which time series we want to look at, let's get the
    // base time series. This has the most values, and is the easiest to get.
    val baseTimeSeries = connection.getTimeSeriesEntries(selectedArchive.get.metadataKey, eventType, timeRange = Some(timeRange))
    if (baseTimeSeries.isFailure) {
      logger.error(s"Failed to get time series: ${baseTimeSeries.failed.get}")
      return
    }
    logger.info(s"Got ${baseTimeSeries.get.size} entries for base time series of type $eventType with key ${selectedArchive.get.metadataKey} in last $timeRange seconds")
    logger.info(s"Head: ${baseTimeSeries.get.head.toString}")

    // We'll also grab one of the summary fields. Don't really care which one,
    // except that I don't want the one with the same duration as timeRange.
    // This one works.
    val selectedSummary = selectedEvent.get.summaries.find(_.summaryWindow == 3600)
    if (selectedSummary.isEmpty) {
      logger.error(s"Couldn't find selected summary for time series of type $eventType with key ${selectedArchive.get.metadataKey}")
      return
    }
    val summaryTimeSeries = connection.getTimeSeriesSummaryEntries(selectedSummary.get, timeRange = Some(timeRange))
    if (summaryTimeSeries.isFailure) {
      logger.error(s"Failed to get summary time series: ${summaryTimeSeries.failed.get}")
      return
    }
    logger.info(s"Got ${summaryTimeSeries.get.size} entries for summarised time series of type $eventType with key ${selectedArchive.get.metadataKey} in last $timeRange seconds")
    logger.info(s"Head: ${summaryTimeSeries.get.head.toString}")
  }

  val serialiseLocation = "/tmp/sevm/archive.spkl"
  val timeToCheck: Duration = Duration.ofDays(7)

  def serialiseTheResults(): Iterable[Archive] = {
    connection.getArchiveList(timeRange = Some(timeToCheck.getSeconds)) match {
      case Failure(exception) => logger.error(exception.toString); Seq()
      case Success(value) =>
        val f = new File(serialiseLocation)
        Try(f.delete())
        f.getParentFile.mkdirs()
        val oos = new ObjectOutputStream(new FileOutputStream(serialiseLocation))
        oos.writeObject(value)
        oos.close()
        logger.info("Done!")
        value
    }
  }

  def exploreTheResults(): Unit = {
    val a = {
      Try(new ObjectInputStream(new FileInputStream(serialiseLocation)).readObject().asInstanceOf[Iterable[Archive]]) match {
        case Failure(exception) => logger.error(exception.toString); serialiseTheResults()
        case Success(value) => value
      }
    }

    def summarise[B](func: Archive => Iterable[B]): Map[B, Int] = {
      a.flatMap(func).groupBy(identity).mapValues(_.size)
    }

    val subjectTypes = summarise(_.subjectType)

    val sd = summarise(s => Seq(s.inputSource, s.inputDestination))
    val sdIsAllDenv = sd.map(_.toString).map(_.contains("denv")).groupBy(identity).mapValues(_.size)
    val noDenvWithIndex = a.zipWithIndex.find(s => !s._1.inputSource.getOrElse("").contains("denv") && !s._1.inputDestination.getOrElse("").contains("denv"))
    // Note that all but one entry has denv as either a source or a destination, meaning that an endpoint API only
    // serves data related to itself.

    val toolNames = summarise(_.toolName)
    val transportProtocols = summarise(_.ipTransportProtocol)
    val pschedulerTestTypes = summarise(_.pschedulerTestType)

    val eventTypes = summarise(_.eventTypes.map(_.eventType))
    val summaryTypes = summarise(_.eventTypes.flatMap(_.summaries.map(_.summaryType)))

    val exampleOfE = eventTypes.keys
      .map(t => (t, a.find(_.eventTypes.exists(e => e.eventType == t && Instant.ofEpochSecond(e.timeUpdated.getOrElse(0).toLong).isAfter(Instant.now().minus(timeToCheck))))))
      .map(t => (t._1, t._2, t._2.map(_.eventTypes.find(e => e.eventType == t._1 && Instant.ofEpochSecond(e.timeUpdated.getOrElse(0).toLong).isAfter(Instant.now().minus(timeToCheck))))))
      .map(t => (t._1, t._2, t._3, t._3.map(_.map { et =>
        Thread.sleep(5)
        connection.getTimeSeriesEntries(et.metadataKey, et.eventType, timeRange = Some(Duration.ofDays(1).getSeconds))
      })))

    val i = 1
  }

  /** This function makes a PollingEsmondSourceFunction, and does some testing on it. */
  def useSourceFunction(): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.getConfig.setGlobalJobParameters(Configuration.get(Array()))

    env.setParallelism(1)

    env
      .addSource(new PollingEsmondSourceFunction())
      .name("Esmond Source Function")
      .print("Esmond")

    env.execute("Esmond Runner")
  }

  def main(args: Array[String]): Unit = {
    //useAllTheCalls()
    //useSourceFunction()
    //serialiseTheResults()
    exploreTheResults()
  }
}
