/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
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
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.connectors.influx.LineProtocol

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import scala.io.Source

/** I used this to generate amp2.lproto.gz from some larger export dumps I took
  * from a running amplet2-collector. It filters down the huge dump to one with
  * a bearable number of measurements of each type. It'll either aim for up to
  * a thousand measurements of each type, or a minimum of three measurements
  * per unique tag set for those measurement types that have a lot of entries.
  */
object SimplifyInfluxExport {
  def main(args: Array[String]): Unit = {
    val maxMeasurementsPerType = 1000

    def entriesFrom(file: String) = Source
      .fromInputStream(new GZIPInputStream(
        new FileInputStream(s"src/test/resources/$file.lproto.gz")
      ))
      .getLines()
      .drop(7)

    val writer = new BufferedWriter(
      new OutputStreamWriter(
        new GZIPOutputStream(
          new FileOutputStream("src/test/resources/amp2.lproto.gz")
        )
      )
    )

    Seq(
      ("external", "export"),
      ("fastping", "export"),
      ("http", "export"),
      ("latency-dns", "export-littler"),
      ("latency-icmp", "export-littler"),
      ("latency-tcpping", "export-littler"),
      ("pathlen", "export-little"),
      ("sip", "export-little"),
      ("throughput", "export"),
      ("traceroute", "export-littler"),
      ("udpstream", "export"),
      ("video", "export")
    )
      .flatMap { case (t, file) =>
        println(t, file)
        val groupedByTags = entriesFrom(file)
          // Get only the entries of the type we're looking for
          .filter { e =>
            if (t.startsWith("latency")) {
              e.startsWith("latency") && e.contains(s"test=${t.split('-')(1)}")
            }
            else {
              e.startsWith(t)
            }
          }
          // Parse the line protocol
          .map(l => (LineProtocol(l).get, l))
          .toList
          // Group the streams together
          .groupBy(_._1.tags)
          .mapValues { lines =>
            lines
              // Pair up any entries with the same tags and times
              // Together, these make up each measurement
              .groupBy(_._1.time)
              // Merge them together - each entry has just one
              // field, so that's the only part of the LineProtocol
              // we need to adjust when merging.
              .map { case (_, items) =>
                type T = (LineProtocol, String)
                items.reduce { (first: T, second: T) =>
                  val mergedProto = LineProtocol(
                    first._1.measurementName,
                    first._1.tags,
                    first._1.fields ++ second._1.fields,
                    first._1.time
                  )
                  (mergedProto, mergedProto.toString)
                }
              }
              // Sort them by time and grab just the line protocol strings
              .toList
              .sortBy(_._1.time)
              .map(_._2)
          }

        // Grab only a few per stream
        // We want an equal amount of each stream, and we usually try to go for
        // a maximum of `maxMeasurementsPerType` for the entire group here.
        // However, if there's enough tag sets that you'd get very few
        // measurements in each stream, we put a minimum in of three per stream.
        println(s"${groupedByTags.size} tag sets")
        groupedByTags
          .flatMap { case (_, items) =>
            items.take(
              Math.max(
                Math.ceil(maxMeasurementsPerType.toDouble / groupedByTags.size).toInt,
                3
              )
            )
          }
      }
      .foreach { line =>
        writer.write(line)
        writer.newLine()
      }

    writer.flush()
    writer.close()
  }
}
