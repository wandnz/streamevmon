/* This file is part of streamevmon.
 *
 * Copyright (C) 2020-2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.tuner.nab

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.Logging

import java.io._
import java.nio.file.{Files, Paths}

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.compat.Platform.EOL

/** Writes detector results to file in the format required by the NAB scorer.
  *
  * Requires `./data/NAB/results` to be present.
  *
  * @param outputLocation The folder to put the results for this instance.
  * @param inputFile      The input NAB data file that this test is being run from.
  */
class NabScoringFormatSink(outputLocation: String, inputFile: File, detectorName: String) extends RichSinkFunction[Event] with
                                                                                                  Logging {

  type FormattedType = (String, String, String, String)

  private val scoreScalingMode = NabScoreScalingMode.withName(
    System.getProperty("nz.net.wand.streamevmon.tuner.nabScoreScalingMode")
  )
    .asInstanceOf[NabScoreScalingMode.ScalingValue]

  val outputSubdir: String = inputFile.getParentFile.getName
  val outputFilename = s"${detectorName}_${inputFile.getName}"

  var unprocessedMeasurements: Option[mutable.Queue[FormattedType]] = None

  val filename = s"$outputLocation/$outputSubdir/$outputFilename"

  /** Reads the file from `./data/NAB/results` to get the correct labels and
    * values and such. Only reads from disk if the results are not cached in
    * memory.
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

  var invocationCount = 0

  override def invoke(value: Event, context: SinkFunction.Context): Unit = {
    readExampleResults()
    // Whenever we receive an event, we'll output all the input measurements up
    // until that time. The one corresponding to the new event will have its
    // severity copied over.
    outputMeasurements(formatMeasurements(Some(value)))

    // Certain conditions might cause us to overwrite the file instead of
    // appending in invocations after the first, which loses most of our data.
    // This incurs a cost on the filesystem, but that's an acceptable tradeoff
    // for the security of knowing that the output file is (more likely to be)
    // valid.
    invocationCount += 1
    val reader = new BufferedReader(new FileReader(new File(filename)))
    val line = reader.readLine
    if (!line.contains("timestamp")) {
      throw new Throwable(s"After $invocationCount invocations, the first line of our output file became invalid! $filename $line")
    }
  }

  override def open(parameters: Configuration): Unit = {
    new File(filename).getParentFile.mkdirs()
    new File(filename).delete()
  }

  override def close(): Unit = {
    readExampleResults()
    // Here we need to empty the unprocessedMeasurements queue to get every
    // input measurement on disk.
    outputMeasurements(formatMeasurements(None))

    val reader = new BufferedReader(new FileReader(new File(filename)))
    val line = reader.readLine
    if (!line.contains("timestamp")) {
      throw new Throwable(s"Invalid first line of finished output file $filename! $line")
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
        // Otherwise, just keep on going. Measurements that we don't have events
        // for have 0 severity.
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

  /** Writes formatted measurements to the file. The writer is created if it
    * doesn't already exist.
    */
  def outputMeasurements(formatted: Iterable[FormattedType]): Unit = {
    val fStream = new FileOutputStream(new File(filename), true)
    val oStream = new OutputStreamWriter(fStream)

    formatted.foreach { f =>
      oStream.write(f.productIterator.mkString(","))
      oStream.write(EOL)
    }

    oStream.flush()
    oStream.close()
  }
}
