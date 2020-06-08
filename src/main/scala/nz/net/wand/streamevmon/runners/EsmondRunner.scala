package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.connectors.esmond.EsmondConnectionForeground
import nz.net.wand.streamevmon.Logging

object EsmondRunner extends Logging {
  val timeRange = 86400
  val eventType = "packet-count-sent"

  private val connection = EsmondConnectionForeground("denv-owamp.es.net")

  def main(args: Array[String]): Unit = {
    // Let's get the full list of supported time series within the relevant timeRange.
    val archiveFull = connection.getArchiveList(timeRange = Some(timeRange))

    // If it crashed out, then we can't continue.
    if (archiveFull.isFailure) {
      logger.error(s"Exception getting archive list: ${archiveFull.failed.get}")
      return
    }

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

    // Now that we know which time series we want to look at, let's get the
    // base time series. This has the most values, and is the easiest to get.
    val baseTimeSeries = connection.getTimeSeries(selectedArchive.get.metadataKey, eventType, timeRange)
    if (baseTimeSeries.isFailure) {
      logger.error(s"Failed to get time series: ${baseTimeSeries.failed.get}")
      return
    }
    logger.info(s"Got ${baseTimeSeries.get.length} entries for base time series of type $eventType with key ${selectedArchive.get.metadataKey} in last $timeRange seconds")
    logger.info(s"Head: ${baseTimeSeries.get.head.toString}")

    // We'll also grab one of the summary fields. Don't really care which one,
    // except that I don't want the one with the same duration as timeRange.
    // This one works.
    val selectedSummary = selectedEvent.get.summaries.find(_.summaryWindow == 3600)
    if (selectedSummary.isEmpty) {
      logger.error(s"Couldn't find selected summary for time series of type $eventType with key ${selectedArchive.get.metadataKey}")
      return
    }
    val summaryTimeSeries = connection.getTimeSeriesSummary(selectedSummary.get)
    if (summaryTimeSeries.isFailure) {
      logger.error(s"Failed to get summary time series: ${summaryTimeSeries.failed.get}")
      return
    }
    logger.info(s"Got ${summaryTimeSeries.get.length} entries for base time series of type $eventType with key ${selectedArchive.get.metadataKey} in last $timeRange seconds")
    logger.info(s"Head: ${summaryTimeSeries.get.head.toString}")
  }
}
