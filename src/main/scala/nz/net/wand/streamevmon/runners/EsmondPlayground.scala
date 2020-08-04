package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.connectors.esmond.EsmondConnectionForeground
import nz.net.wand.streamevmon.connectors.esmond.schema.{AbstractTimeSeriesEntry, Archive, EventType}
import nz.net.wand.streamevmon.flink.sources.PollingEsmondSourceFunction
import nz.net.wand.streamevmon.measurements.esmond.{EsmondMeasurement, RichEsmondMeasurement}

import java.io._
import java.time.{Duration, Instant}
import java.util.concurrent.ForkJoinPool

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.FileUtils
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.parallel.{ForkJoinTaskSupport, ParIterable}
import scala.util.{Failure, Success, Try}

object EsmondPlayground extends Logging {
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
    val baseTimeSeries = connection.getTimeSeriesEntriesFromMetadata(selectedArchive.get.metadataKey, eventType, timeRange = Some(timeRange))
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
  val timeToCheck: Duration = Duration.ofSeconds(600000)

  def serialiseTheResults(): Iterable[Archive] = {
    connection.getArchiveList(timeRange = Some(3600)) match {
      case Failure(exception) => logger.error(exception.toString); Seq()
      case Success(value) =>
        val f = new File(serialiseLocation)
        Try(f.delete())
        f.getParentFile.mkdirs()
        val oos = new ObjectOutputStream(new FileOutputStream(serialiseLocation))
        oos.writeObject(value)
        oos.close()
        logger.info("Done writing spickle!")

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
      .map(t => (t, a.find(ar => ar.inputDestination.getOrElse("").contains("aofa") && ar.eventTypes.exists(e => e.eventType == t))))
      .map(t => (t._1, t._2, t._2.map(_.eventTypes.find(e => e.eventType == t._1)))) //&& Instant.ofEpochSecond(e.timeUpdated.getOrElse(0).toLong).isAfter(Instant.now().minus(timeToCheck))))))
      .map(t => (t._1, t._2, t._3, t._3.map(_.map { et =>
        var retVal: Option[Try[Iterable[AbstractTimeSeriesEntry]]] = None
        var count = 0
        while (retVal.isEmpty) {
          Thread.sleep(3000)
          val result = connection.getTimeSeriesEntriesFromMetadata(et.metadataKey, et.eventType, timeRange = Some(3600))
          result match {
            case Failure(f) => count += 1
              if (count > 10) {
                retVal = Some(result)
              }
            case Success(_) => retVal = Some(result)
          }
        }
        retVal.get
      })))

    val measurements = exampleOfE
      .map { t =>
        (t._1, t._2, t._3, t._4, t._4.map(_.map(_.map(_.map { e =>
          (EsmondMeasurement(t._3.get.get, e), RichEsmondMeasurement(t._3.get.get, e))
        }))))
      }

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val out = new FileWriter("out/esmond.json")
    mapper.writeValue(out, measurements)
    out.close()

    val breakpoint = 1
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

  def storeTheResults(): Unit = {
    val a = {
      Try(new ObjectInputStream(new FileInputStream(serialiseLocation)).readObject().asInstanceOf[Iterable[Archive]]) match {
        case Failure(exception) => logger.error(exception.toString); serialiseTheResults()
        case Success(value) => value
      }
    }

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val filtered = a.filter { ar =>
      ar.inputDestination.getOrElse("").contains("kans")
    }

    val arOut = new FileWriter("out/esmondOverTime/archives.json")
    mapper.writeValue(arOut, filtered)
    arOut.close()

    val baseEvents = filtered.flatMap(ar => ar.eventTypes)

    val results = baseEvents.map { et =>
      var retVal: Option[(EventType, Try[Iterable[AbstractTimeSeriesEntry]])] = None
      var count = 0
      while (retVal.isEmpty) {
        Thread.sleep(3000)
        val result = connection.getTimeSeriesEntriesFromMetadata(et.metadataKey, et.eventType, timeRange = Some(3600))
        result match {
          case Failure(f) => count += 1
            if (count > 10) {
              retVal = Some((et, result))
            }
          case Success(_) => retVal = Some((et, result))
        }
      }
      retVal
    }

    val resOut = new FileWriter(s"out/esmondOverTime/results-${Instant.now()}.json")
    mapper.writeValue(resOut, results)
    resOut.close()

    logger.info("Done writing JSON!")
  }

  def serialiseAllTheArchives(): Unit = {
    // Configure some settings
    val outputFolder = "out/allEsmondArchives"
    val endpointSuffix = "owamp"
    val tRange = Some(Duration.ofDays(1).getSeconds)

    // Set up the JSON output module
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val validEndpoints = Seq(
      "pnwg", "bois", "sacr", "sunn", "lsvn", "denv", "albq", "elpa", "kans",
      "hous", "star", "chic", "nash", "atla", "eqx-ash", "wash", "aofa", "bost",
      "newy", "lond", "amst", "cern-773", "cern-513",
      "anl", "lbl", "ameslab", "fnal", "ga", "llnl", "pppl", "slac", "snll", "ornl",
      "netl-alb", "snla", "pantex", "doe-gtn", "forr", "netl-mgn", "orau", "srs", "netl-pgh", "eqx-sj", "osti",
      "east-dc",
      "test"
      // "eqx-chi" appears to be unreachable
    )

    val netbeamDevices = Seq(
      "albq-asw1", "albq-cr5", "albq-mpr1", "ameslab-rt1", "ameslab-rt2",
      "ameslab-ssw1", "amst-asw1", "amst-cr5", "anl-asw1", "anl-mr2",
      "anl221-mpr1", "anl541b-mpr1", "aofa-asw1", "aofa-cr5", "atla-asw1",
      "atla-cr5", "atla-mpr1", "bnl-lsw1", "bnl-lsw2", "bnl-lsw3", "bnl-lsw5",
      "bnl-lsw6", "bois-asw1", "bois-cr1", "bois-mpr1", "bost-asw1", "bost-cr5",
      "bost-mpr1", "cern-513-asw1", "cern-513-cr5", "cern-773-asw1",
      "cern-773-cr5", "chat-mpr1", "chic-asw1", "chic-cr55", "chic-mpr1",
      "clev-mpr1", "denv-asw1", "denv-cr5", "denv-mpr1", "doe-gtn-rt2",
      "doe-gtn-ssw1", "elpa-asw1", "elpa-cr5", "elpa-mpr1", "eqx-ash-asw1",
      "eqx-ash-cr5", "eqx-chi-asw1", "eqx-chi-cr5", "eqx-sj-asw1", "eqx-sj-cr5",
      "eqxch2-mpr1", "eqxdc4-mpr1", "eqxsv5-mpr1", "esnet-east-rt1",
      "esnet-west-rt1", "fnal-asw1", "fnal-rt1", "fnal-rt2", "fnalfcc-mpr1",
      "fnalgcc-mpr1", "forr-rt2", "forr-ssw1", "ga-asw1", "ga-rt1", "ga-rt3",
      "hous-asw1", "hous-cr5", "hous-mpr1", "kans-asw1", "kans-cr5",
      "kans-mpr1", "lasv-mpr1", "lbl-1165-lsw1", "lbl-1165a-lsw1",
      "lbl-2002-asw1", "lbl-2002-ece1", "lbl-2002-ece2", "lbl-2002-lsw1",
      "lbl-2002-lsw2", "lbl-2002-lsw3", "lbl-2002-lsw4", "lbl-2002-lsw5",
      "lbl-2002-lsw6", "lbl-2002-lsw7", "lbl-2002-ssw1", "lbl-2275-ssw1",
      "lbl-mr2", "lbnl50-mpr1", "lbnl59-mpr1", "llnl-mpr1", "llnl-mr2",
      "llnl-ssw1", "llnldc-rt5", "llnldc-ssw1", "lond-asw1", "lond-cr5",
      "losa-mpr1", "lsvn-cr1", "lsvn-ssw1", "nash-asw1", "nash-cr5",
      "nash-mpr1", "netl-alb-rt1", "netl-alb-ssw1", "netl-mgn-rt1",
      "netl-mgn-ssw1", "netl-pgh-rt1", "netl-pgh-ssw1", "newy-asw1", "newy-cr5",
      "newy1118th-mpr1", "newy32aoa-mpr1", "orau-rt4", "orau-ssw1", "ornl-asw1",
      "ornl-cr5", "ornl-rt4", "ornl-ssw2", "osti-asw1", "osti-rt3",
      "pantex-rt4", "pantex-ssw1", "phil-mpr1", "pnwg-asw1", "pnwg-cr5",
      "pppl-rt5", "pppl-ssw1", "sacr-asw1", "sacr-cr5", "sacr-mpr1",
      "salt-mpr1", "sand-mpr1", "seat-mpr1", "slac-asw1", "slac-mr2",
      "slac50n-mpr1", "slac50s-mpr1", "snla-rt1", "snla-rt2", "snla-ssw1",
      "snlca-mpr1", "snll-asw1", "snll-mr3", "srs-rt1", "srs-rt2", "srs-ssw1",
      "srs-ssw2", "star-asw1", "star-cr5", "star-mpr1", "star-ssw2",
      "star-ssw3", "sunn-asw1", "sunn-cr5", "sunn-cr55", "sunn-mpr1",
      "wash-asw1", "wash-cr5", "wash-mpr1"
    )
    val netbeamDevicesWithoutSuffix = netbeamDevices.map(_.split('-').dropRight(1).mkString("-")).toSet

    // Create connections to all endpoints
    val connections = validEndpoints
      .map { endpoint =>
        if (endpoint == "test") {
          (endpoint, new EsmondConnectionForeground(s"http://$endpoint-${endpointSuffix}1.es.net:8085"))
        }
        else {
          (endpoint, new EsmondConnectionForeground(s"http://$endpoint-$endpointSuffix.es.net:8085"))
        }
      }

    // Do an archiveList request for all endpoints (with parallelism equal to the number of endpoints to make it go quick)
    val archives = {
      val parallelCollection = connections.par
      parallelCollection.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(connections.size))
      parallelCollection.map(conn => (conn._1, conn._2.getArchiveList(timeRange = tRange)))
    }

    // Output the raw archive listings to JSON, one file per endpoint
    def writeToJson(items: Iterable[(String, Any)], folder: String): Unit = {
      // Clean the output folder
      FileUtils.deleteDirectory(new File(s"$outputFolder/$folder"))
      new File(s"$outputFolder/$folder").mkdirs()

      items.foreach { ar =>
        ar._2 match {
          case Failure(exception) => logger.error(s"${ar._1} exception!: $exception")
          case _ =>
        }

        val outputFile = new FileWriter(s"$outputFolder/$folder/${ar._1}-$endpointSuffix.json")
        mapper.writeValue(outputFile, ar._2)
        outputFile.close()
      }
    }

    def writeToJsonPar(items: ParIterable[(String, Any)], folder: String): Unit = writeToJson(items.seq, folder)

    writeToJsonPar(archives, "raw")

    // Group the archive listings by the other end of the test
    val grouped = archives.map { archive =>
      (
        archive._1,
        archive._2 match {
          case Success(value) => SortedMap[String, Iterable[Archive]]() ++ value.groupBy { ar =>
            if (ar.inputSource.getOrElse("").contains(archive._1)) {
              val dest = ar.inputDestination.getOrElse("Unknown")
              if (dest.contains("es.net") && netbeamDevicesWithoutSuffix.exists(dev => dest.contains(dev))) {
                dest
              }
              else {
                "Irrelevant"
              }
            }
            else {
              val src = ar.inputSource.getOrElse("Unknown")
              if (src.contains("es.net") && netbeamDevicesWithoutSuffix.exists(dev => src.contains(dev))) {
                src
              }
              else {
                "Irrelevant"
              }
            }
          }
          case Failure(_) => SortedMap[String, Iterable[Archive]]()
        }
      )
    }.map(a => (a._1, a._2.filterNot(_._1 == "Irrelevant")))

    writeToJsonPar(grouped.asInstanceOf[ParIterable[(String, Any)]], "grouped")

    // We want to make some adjacency graphs here.
    // One should be for pscheduler/powstream archives, and the other for pscheduler/traceroute.
    // First, we should make some adjacency lists.

    def filterArchivesByTool(l: ParIterable[(String, SortedMap[String, Iterable[Archive]])], toolName: String): ParIterable[(String, SortedMap[String, Archive])] = {
      l.map {
        case (hostName, items) => (hostName,
          items.flatMap { case (destName, ars) =>
            if (ars.exists(_.toolName.contains(toolName))) {
              Seq((destName, ars.find(_.toolName.contains(toolName)).get))
            }
            else {
              Seq()
            }
          }
        )
      }
    }

    val powstreamOnly = filterArchivesByTool(grouped, "pscheduler/powstream")
    val tracerouteOnly = filterArchivesByTool(grouped, "pscheduler/traceroute")
    val iperfOnly = filterArchivesByTool(grouped, "pscheduler/iperf3")

    // Then, just drop the Archive from the lists. We don't care about them anymore.
    def removeArchives(l: ParIterable[(String, SortedMap[String, Archive])]): Map[String, Iterable[String]] = {
      l.map {
        case (hostName, items) => (hostName,
          items.map { case (name, _) => name }
        )
      }.toMap.seq
    }

    val powstreamNamesOnly = removeArchives(powstreamOnly)
    val tracerouteNamesOnly = removeArchives(tracerouteOnly)
    val iperfNamesOnly = removeArchives(iperfOnly)

    // Let's keep track of which endpoints show up that we didn't query...
    val unseenEndpoints: mutable.Buffer[String] = mutable.Buffer()

    // This'll drop the -owamp.es.net from full endpoint names
    def nameWithoutSuffix(e: String): String = e.split('-').dropRight(1).mkString("-")

    // This function with both create and write to JSON an adjacency matrix. It's
    // a 2D array of boolean values that show whether a particular endpoint has any
    // data from each other endpoint with the relevant test type.
    def outputAdjacencyMatrix(map: Map[String, Iterable[String]], name: String): Unit = {
      val cols = map.keys.toSeq.sorted
      val adjacencyMatrix = cols.map { col =>
        cols.map { c =>
          powstreamNamesOnly(col).map(nameWithoutSuffix).toSeq.foreach { q =>
            if (!validEndpoints.contains(q)) {
              unseenEndpoints.append(q)
            }
          }

          powstreamNamesOnly(col).map(nameWithoutSuffix).toSeq.contains(c)
        }
      }

      val outputFile = new FileWriter(s"$outputFolder/adjacencyMatrix-$name.json")
      mapper.writeValue(outputFile, adjacencyMatrix)
      outputFile.close()
    }

    // Print out the column and row headers in Python list format
    val cols = powstreamNamesOnly.keys.toSeq.sorted
    println(cols.mkString("[\"", "\",\"", "\"]"))

    outputAdjacencyMatrix(powstreamNamesOnly, "powstream")
    outputAdjacencyMatrix(tracerouteNamesOnly, "traceroute")
    outputAdjacencyMatrix(iperfNamesOnly, "iperf")

    // If there's any endpoints we didn't query, print them out
    if (unseenEndpoints.nonEmpty) {
      println(unseenEndpoints.toSet)
    }

    val breakpoint = 1
  }

  def main(args: Array[String]): Unit = {
    //useAllTheCalls()
    //useSourceFunction()
    //serialiseTheResults()
    //exploreTheResults()
    //storeTheResults()
    serialiseAllTheArchives()
  }


  // Esmond
  // Many API endpoints
  // Each has data for their own tests
  // Make list of all hosts we might care about
  // Get archive list for each
  // For each endpoint, take note of what tests are available

  // Netbeam
  // Single API endpoint
  // Many edge/src/to/dst pairs
  // Make list of all hosts we care about
  // Get a certain day tile's traffic for all pairs

  // We now have a list of host pairs with data in both esmond and netbeam.
  // Netbeam can be easily historically queried.
  // Esmond archiveList for a time range will only return archives that were
  // last updated within that time range. Thus, archive discovery must be done
  // up until the present time regardless of how far in the past we actually
  // want to query.

  // Netbeam can easily be queried by tile as far back as we want.
  // Esmond appears to only return up to 1500 measurements at once, which is
  // just over a day worth of measurements if they're taken every 60s.

  // We could make a function that does a simple day-long query, detects
  // measurement frequency, then goes ahead and makes sequential queries of
  // the appropriate range to get about a thousand measurements at a time
  // without overlap.

  // We should think about how to store these. We likely want them in both
  // JSON and spickle format. Each host has a set of archives, each archive
  // has a set of event types, and each event type might have a set of summaries.
  // We could make this one gigantic object/file, but we probably want to split
  // it into folders.

  // Each folder should be named after a UID, and contains one file for the
  // descriptor whose UID it's named after. It can contain a number of folders
  // for each sub-descriptor, and if it's a descriptor of a time series, it
  // should also contain a file or a number of files for the values contained
  // in the time series.

  val validPairs = Seq(
    ("albq", "denv"),
    ("albq", "elpa"),
    ("ameslab", "chic"),
    ("ameslab", "star"),
    ("amst", "bost"),
    ("amst", "cern-513"),
    ("amst", "lond"),
    ("anl", "chic"),
    ("anl", "star"),
    ("aofa", "newy"),
    ("aofa", "pppl"),
    ("aofa", "star"),
    ("aofa", "wash"),
    ("atla", "nash"),
    ("atla", "ornl"),
    ("atla", "wash"),
    ("bois", "denv"),
    ("bois", "pnwg"),
    ("bost", "amst"),
    ("bost", "newy"),
    ("bost", "star"),
    ("cern-513", "amst"),
    ("cern-513", "cern-773"),
    ("cern-773", "cern-513"),
    ("cern-773", "lond"),
    ("chic", "ameslab"),
    ("chic", "anl"),
    ("chic", "fnal"),
    ("chic", "kans"),
    ("chic", "nash"),
    ("chic", "sacr"),
    ("chic", "star"),
    ("chic", "wash"),
    ("denv", "albq"),
    ("denv", "bois"),
    ("denv", "kans"),
    ("denv", "lsvn"),
    ("denv", "pnwg"),
    ("denv", "sacr"),
    ("elpa", "albq"),
    ("elpa", "ga"),
    ("elpa", "hous"),
    ("elpa", "sunn"),
    ("fnal", "chic"),
    ("fnal", "star"),
    ("ga", "elpa"),
    ("ga", "lsvn"),
    ("ga", "sunn"),
    ("hous", "elpa"),
    ("hous", "kans"),
    ("hous", "nash"),
    ("hous", "pantex"),
    ("kans", "chic"),
    ("kans", "denv"),
    ("kans", "hous"),
    ("lbl", "sacr"),
    ("lbl", "sunn"),
    ("llnl", "sacr"),
    ("llnl", "snll"),
    ("llnl", "sunn"),
    ("llnl", "wash"),
    ("lond", "amst"),
    ("lond", "cern-773"),
    ("lsvn", "denv"),
    ("lsvn", "ga"),
    ("lsvn", "sunn"),
    ("nash", "atla"),
    ("nash", "chic"),
    ("nash", "hous"),
    ("nash", "ornl"),
    ("nash", "wash"),
    ("newy", "aofa"),
    ("newy", "bost"),
    ("orau", "ornl"),
    ("ornl", "atla"),
    ("ornl", "nash"),
    ("ornl", "orau"),
    ("ornl", "osti"),
    ("osti", "ornl"),
    ("pantex", "hous"),
    ("pnwg", "bois"),
    ("pnwg", "denv"),
    ("pnwg", "sacr"),
    ("pppl", "aofa"),
    ("pppl", "wash"),
    ("sacr", "chic"),
    ("sacr", "denv"),
    ("sacr", "lbl"),
    ("sacr", "llnl"),
    ("sacr", "pnwg"),
    ("sacr", "slac"),
    ("sacr", "snll"),
    ("sacr", "sunn"),
    ("slac", "sacr"),
    ("slac", "sunn"),
    ("snll", "llnl"),
    ("snll", "sacr"),
    ("snll", "sunn"),
    ("star", "ameslab"),
    ("star", "anl"),
    ("star", "aofa"),
    ("star", "bost"),
    ("star", "chic"),
    ("star", "fnal"),
    ("sunn", "elpa"),
    ("sunn", "ga"),
    ("sunn", "lbl"),
    ("sunn", "llnl"),
    ("sunn", "lsvn"),
    ("sunn", "sacr"),
    ("sunn", "slac"),
    ("sunn", "snll"),
    ("wash", "aofa"),
    ("wash", "atla"),
    ("wash", "chic"),
    ("wash", "llnl"),
    ("wash", "nash"),
    ("wash", "pppl"),
  )
}
