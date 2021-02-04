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

package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP

import org.apache.flink.api.common.io.GenericCsvInputFormat

import scala.collection.mutable

/** An InputFormat which parses the AMP ICMP results from the Latency TS I
  * dataset.
  *
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
class LatencyTSAmpFileInputFormat extends GenericCsvInputFormat[LatencyTSAmpICMP]
                                          with HasFlinkConfig {

  override val flinkName: String = "Latency TS Amp Source"
  override val flinkUid: String = "latency-ts-amp-source"
  override val configKeyGroup: String = "latencyts.ampicmp"

  override def openInputFormat(): Unit = {
    // We need parallelism 1 because we want to make unique stream IDs.
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this InputFormat must be 1.")
    }
  }

  val recordToStream: mutable.Map[String, Int] = mutable.Map()

  override def readRecord(
    reuse: LatencyTSAmpICMP,
    bytes: Array[Byte],
    offset: Int,
    numBytes: Int
  ): LatencyTSAmpICMP = {

    val line = new String(bytes.slice(offset, offset + numBytes))
    val key = line.split(",")(0)
    val stream = recordToStream.getOrElseUpdate(key, recordToStream.size)

    LatencyTSAmpICMP.create(line, stream.toString)
  }
}
