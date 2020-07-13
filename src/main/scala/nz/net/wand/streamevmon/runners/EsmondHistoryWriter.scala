package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.connectors.esmond.{AbstractEsmondConnection, EsmondAPI, EsmondConnectionForeground}
import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.connectors.esmond.schema._

import java.io._
import java.util.concurrent.ForkJoinPool

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.io.FileUtils

import scala.collection.parallel.ForkJoinTaskSupport
import scala.util.{Failure, Success}

object EsmondHistoryWriter extends Logging {

  private val lastTile = 18451
  private val numDays = 7
  private val firstTile = lastTile - numDays

  private val takeLimit = Int.MaxValue

  val firstTimestamp: Option[Long] = Some(EsmondAPI.tileToTimeRange(firstTile)._1.getEpochSecond)
  val lastTimestamp: Option[Long] = Some(EsmondAPI.tileToTimeRange(lastTile)._2.getEpochSecond)

  val timestampsInOrder: Seq[(Long, Long)] = Range(0, numDays).reverse.map { dayOffset =>
    println(lastTile - dayOffset)
    val range = EsmondAPI.tileToTimeRange(lastTile - dayOffset)
    (
      range._1.getEpochSecond,
      range._2.getEpochSecond
    )
  }

  // Drops the -owamp.es.net or other suffix from full endpoint names, assuming there's only any - in the lowest level domain segment.
  def nameWithoutSuffix(e: String): String = e.split('-').dropRight(1).mkString("-")

  // Gets whichever string doesn't contain src, or ""
  def getOtherEndFromStrings(src: String, in1: Option[String], in2: Option[String]): String = {
    if (in1.getOrElse("").contains(src)) {
      in2.getOrElse("")
    }
    else if (in2.getOrElse("").contains(src)) {
      in1.getOrElse("")
    }
    else {
      ""
    }
  }

  def getOtherEnd(src: String, in: Archive): String = {
    getOtherEndFromStrings(src, in.inputSource, in.inputDestination)
  }

  // Gets all the relevant time series entries for a particular EventType according to the configured tiles.
  def getTimeSeriesEntries(
    conn: AbstractEsmondConnection,
    ev  : EventType
  ): Iterable[AbstractTimeSeriesEntry] = {
    var failureCount = 0
    timestampsInOrder
      .flatMap {
        case (timeStart, timeEnd) =>
          var retVal: Option[Iterable[AbstractTimeSeriesEntry]] = None
          while (retVal.isEmpty) {
            val result =
              conn.getTimeSeriesEntries(ev, timeStart = Some(timeStart), timeEnd = Some(timeEnd))
            result match {
              case Failure(exception) =>
                failureCount += 1
                logger.warn(s"Failure #$failureCount getting time series entry: $exception")
                Thread.sleep(5000)
              case Success(value) => retVal = Some(value)
            }
          }
          retVal.get
      }
      .sortBy(_.timestamp)
  }

  // Gets all the relevant time series entries for a particular Summary according to the configured tiles.
  def getTimeSeriesEntries(
    conn: AbstractEsmondConnection,
    su  : Summary
  ): Iterable[AbstractTimeSeriesEntry] = {
    var failureCount = 0
    timestampsInOrder
      .flatMap {
        case (timeStart, timeEnd) =>
          var retVal: Option[Iterable[AbstractTimeSeriesEntry]] = None
          while (retVal.isEmpty) {
            val result = conn.getTimeSeriesSummaryEntries(su,
              timeStart = Some(timeStart),
              timeEnd = Some(timeEnd))
            result match {
              case Failure(exception) =>
                failureCount += 1
                logger.warn(s"Failure #$failureCount getting time series entry: $exception")
                Thread.sleep(5000)
              case Success(value) => retVal = Some(value)
            }
          }
          retVal.get
      }
      .sortBy(_.timestamp)
  }

  def main(args: Array[String]): Unit = {
    // Grab the valid src:dst pairs that we care about.
    val pairs = EsmondPlayground.validPairs
    // Group them by their source so we know how to query them properly.
    val groupedPar = pairs.groupBy(_._1).mapValues(_.map(_._2)).par
    // Set up a new ForkJoinPool to handle concurrent operations with enough
    // threads to query every endpoint at the same time.
    val taskSupport = new ForkJoinTaskSupport(new ForkJoinPool(groupedPar.size))

    // We have to set the taskSupport for every new collection we make, since
    // it doesn't get passed down to children :(
    groupedPar.tasksupport = taskSupport
    // Make an EsmondConnection for each unique endpoint.
    val connections = groupedPar.map(a =>
      (a._1, new EsmondConnectionForeground(s"http://${a._1}-owamp.es.net:8085"), a._2))

    logger.info("Made connections")

    // Query each endpoint for all the archives that have a lastUpdated value
    // since the start of the first tile we care about.
    connections.tasksupport = taskSupport
    val archives = connections.map {
      case (src, conn, ends) =>
        (
          src,
          conn,
          ends,
          conn.getArchiveList(timeStart = firstTimestamp)
        )
    }

    logger.info("Got all archive lists")

    // Get rid of all the archives representing measurements with an endpoint
    // we don't care about.
    val filteredArchives = archives.map {
      case (src, conn, ends, ars) =>
        (
          src,
          conn,
          ends,
          ars match {
            case Failure(exception) =>
              logger.error(s"Failed archiveList query for $src! $exception"); Iterable()
            case Success(value) =>
              value.filter { ar =>
                val otherEnd = getOtherEnd(src, ar)
                ends.exists(end => nameWithoutSuffix(otherEnd).contains(end))
              }
          }
        )
    }

    logger.info("Filtered archive lists")

    // Group the archives depending on their other end.
    // We now have the endpoint hostname, an active EsmondConnection, a list of
    // the names of the relevant test destinations, a flat list of the relevant
    // archives, and a map of the destination names to their archives.
    val archivesInDstMap = filteredArchives.map {
      case (src, conn, ends, ars) =>
        (
          src,
          conn,
          ends,
          ars,
          ends.map { end =>
            (
              end,
              ars.filter { ar =>
                val otherEnd = getOtherEnd(src, ar)
                nameWithoutSuffix(otherEnd) == end
              }
            )
          }.take(takeLimit).toMap
        )
    }.take(takeLimit)

    logger.info("Grouped archives")

    archivesInDstMap.tasksupport = taskSupport
    val withResponses = archivesInDstMap.map {
      case (src, conn, ends, ars, mappedArs) =>
        (
          src,
          conn,
          ends,
          ars,
          mappedArs,
          mappedArs.map {
            case (k, mars) =>
              (k, mars.map { ar =>
                (
                  ar,
                  ar.eventTypes.take(takeLimit).map { ev =>
                    (
                      ev,
                      getTimeSeriesEntries(conn, ev)
                    )
                  },
                  ar.eventTypes.take(takeLimit).flatMap(_.summaries).map { su =>
                    (
                      su,
                      getTimeSeriesEntries(conn, su)
                    )
                  }
                )
              })
          }
        )
    }

    logger.info("Got all the time series entries")

    val toSerialise: SerialisedT =
      withResponses.seq.map {
        case (src, conn, ends, ars, mappedArs, mappedArsWithValues) =>
          (
            src,
            mappedArsWithValues
          )
      }.toMap

    logger.info("Arranged data for serialisation")

    val breakpoint = 1
    // Stop the forkjoinpool cleanly.
    taskSupport.environment.shutdown()

    logger.info("Stopped forkpool")

    // Set up the folder to output in
    FileUtils.deleteDirectory(new File(serialiseLocation))
    new File(serialiseLocation).mkdirs()

    logger.info("Starting to serialise as one file")
    writeAsOneFile(toSerialise)
    logger.info("Starting to serialise as multiple files")
    writeAsMultipleFiles(toSerialise)
    logger.info("Done!")
  }

  type SerialisedT = Map[String,
    Map[String,
      Iterable[(Archive,
        List[(EventType, Iterable[AbstractTimeSeriesEntry])],
        List[(Summary, Iterable[AbstractTimeSeriesEntry])])]]]

  val serialiseLocation = "out/allEsmondData"

  def writeAsOneFile(in: SerialisedT): Unit = {
    // Set up the JSON output module
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    // Write as json
    val jsonOut = new FileWriter(s"$serialiseLocation/big.json")
    mapper.writeValue(jsonOut, in)
    jsonOut.close()
    logger.info("Wrote big JSON")
    val spickleOut = new ObjectOutputStream(new FileOutputStream(s"$serialiseLocation/big.spkl"))
    spickleOut.writeObject(in)
    spickleOut.close()
    logger.info("Wrote big spickle")
  }

  def writeAsMultipleFiles(in: SerialisedT): Unit = {
    // Set up the JSON output module
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    def write(where: String, what: Any): Unit = {
      new File(s"$serialiseLocation/$where").getParentFile.mkdirs()
      val outJs = new FileWriter(s"$serialiseLocation/$where.json")
      mapper.writeValue(outJs, what)
      outJs.close()
      val outSp = new ObjectOutputStream(new FileOutputStream(s"$serialiseLocation/$where.spkl"))
      outSp.writeObject(what)
      outSp.close()
    }

    var counter = 0
    in.foreach {
      case (src, dsts) =>
        logger.info(s"Writing data for source $counter of ${in.size}")
        counter += 1
        dsts.foreach {
          case (dst, items) =>
            items.foreach {
              case (archive, eventTypes, summaries) =>
                val arWhere = s"$src/$dst/${archive.toolName.getOrElse("unknown-tool")}"
                write(s"$arWhere/archive", archive)

                eventTypes.foreach {
                  case (eventType, entries) =>
                    val evWhere = s"$arWhere/${eventType.eventType}"
                    write(s"$evWhere/eventType", eventType)
                    write(s"$evWhere/baseEntries", entries)
                }
                summaries.foreach {
                  case (summary, entries) =>
                    val evWhere =
                      s"$arWhere/${summary.eventType}/${summary.summaryType}/${summary.summaryWindow}"
                    write(s"$evWhere/summary", summary)
                    write(s"$evWhere/entries", entries)
                }
            }
        }
    }
  }
}

object SecondMain {

  def main(args: Array[String]): Unit = {
    //val serialised = new ObjectInputStream(new FileInputStream("out/allEsmondData/big.spkl"))
    //  .readObject()
    //  .asInstanceOf[EsmondHistoryWriter.SerialisedT]

    //FileUtils.deleteDirectory(new File(EsmondHistoryWriter.serialiseLocation))
    //new File(EsmondHistoryWriter.serialiseLocation).mkdirs()

    //EsmondHistoryWriter.writeAsOneFile(serialised)
    //EsmondHistoryWriter.writeAsMultipleFiles(serialised)

    println(EsmondHistoryWriter.timestampsInOrder)

    val breakpoint = 1
  }
}
