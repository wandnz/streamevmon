package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths}

import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}

import scala.collection.mutable
import scala.collection.JavaConverters._

/** Writes detector results to file in the format required by the NAB scorer.
  *
  * Requires `./data/NAB/results` to be present.
  *
  * @param outputLocation The folder to put the results for this instance.
  * @param inputFile      The input NAB data file that this test is being run from.
  */
class NabScoringFormatSink(outputLocation: String, inputFile: File, detectorName: String) extends RichSinkFunction[Event] {

  type FormattedType = (String, String, String, String)

  private val scoreScalingMode = NabScoreScalingMode.withName(
    System.getProperty("nz.net.wand.streamevmon.tuner.nabScoreScalingMode")
  )
    .asInstanceOf[NabScoreScalingMode.ScalingValue]

  val outputSubdir: String = inputFile.getParentFile.getName
  val outputFilename = s"${detectorName}_${inputFile.getName}"

  var unprocessedMeasurements: Option[mutable.Queue[FormattedType]] = None

  var writer: Option[BufferedWriter] = None

  /** Reads the file from `./data/NAB/results` to get the correct labels and
    * values and such. Idempotent.
    */
  def readExampleResults(): Unit = {
    unprocessedMeasurements match {
      case Some(_) =>
      case None =>
        val examplesFile = s"./data/NAB/results/null/$outputSubdir/null_${inputFile.getName}"

        val allLines = Files.readAllLines(Paths.get(examplesFile))

        val exampleResults = allLines.asScala.map { l =>
          val parts = l.split(",")
          (parts(0), parts(1), parts(2), parts(3))
        }

        unprocessedMeasurements = Some(mutable.Queue(exampleResults: _*))
    }
  }

  override def invoke(value: Event, context: SinkFunction.Context[_]): Unit = {
    readExampleResults()
    // Whenever we receive an event, we'll output all the input measurements up
    // until that time. The one corresponding to the new event will have its
    // severity copied over.
    outputMeasurements(formatMeasurements(Some(value)))
  }

  override def close(): Unit = {
    readExampleResults()
    // Here we need to empty the unprocessedMeasurements queue and close the
    // writer.
    outputMeasurements(formatMeasurements(None))
    writer match {
      case Some(value) =>
        value.flush()
        value.close()
      case None =>
    }
  }

  /** Formats the input stream of measurements, and copies the severity of the
    * given event into the correct place. Stops formatting when it gets to the
    * time matching the event, or the end of the stream.
    */
  def formatMeasurements(event: Option[Event]): Iterable[FormattedType] = {
    val formatted: mutable.Queue[FormattedType] = mutable.Queue()

    var breakOut = false
    while (!breakOut && unprocessedMeasurements.get.nonEmpty) {
      val measurement = unprocessedMeasurements.get.dequeue()
      event match {
        // We need to not touch the header line.
        case _ if measurement._1 == "timestamp" => formatted.enqueue(measurement)
        // If this measurement matches the event time, we stop early.
        case Some(value) if NabMeasurement.formatter.format(value.time) == measurement._1 =>
          breakOut = true
          formatted.enqueue((
            measurement._1,
            measurement._2,
            scoreScalingMode.scale(value.severity).toString,
            measurement._4
          ))
        // Otherwise, just keep on going.
        case _ => formatted.enqueue((
          measurement._1,
          measurement._2,
          "0",
          measurement._4
        ))
      }
    }

    formatted
  }

  /** Writes formatted measurements to the file. Idempotently makes the writer.
    */
  def outputMeasurements(formatted: Iterable[FormattedType]): Unit = {
    writer match {
      case Some(_) =>
      case None =>
        writer = Some {
          val filename = s"$outputLocation/$outputSubdir/$outputFilename"
          new File(filename).getParentFile.mkdirs()
          new File(filename).delete()
          new BufferedWriter(new FileWriter(filename))
        }
    }

    formatted.foreach { f =>
      writer.get.write(f.productIterator.mkString(","))
      writer.get.newLine()
    }
  }
}
