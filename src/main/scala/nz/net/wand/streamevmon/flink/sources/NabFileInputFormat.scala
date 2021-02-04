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
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement

import java.lang

import org.apache.flink.api.common.io.GenericCsvInputFormat
import org.apache.flink.core.fs.FileInputSplit

/** An InputFormat which parses data from the NAB dataset.
  *
  * @see [[https://github.com/numenta/NAB]]
  */
class NabFileInputFormat extends GenericCsvInputFormat[NabMeasurement]
                                 with HasFlinkConfig {

  // All the files have a header, which we don't care about.
  setSkipFirstLineAsHeader(true)

  override val flinkName: String = "NAB Measurement Source"
  override val flinkUid: String = "nab-source"
  override val configKeyGroup: String = "nab"

  private var currentStream: String = "UnknownNabStream"

  // We set the measurement's stream ID to the filename of the input file.
  private def setCurrentStream(split: FileInputSplit): Unit = {
    currentStream = s"${split.getPath.getParent.getName}/${split.getPath.getName}"
  }

  // When we open or reopen, we should make sure to set the stream ID.
  override def open(split: FileInputSplit): Unit = {
    super.open(split)
    setCurrentStream(split)
  }

  override def reopen(split: FileInputSplit, state: lang.Long): Unit = {
    super.reopen(split, state)
    setCurrentStream(split)
  }

  override def readRecord(
    reuse   : NabMeasurement,
    bytes   : Array[Byte],
    offset  : Int,
    numBytes: Int
  ): NabMeasurement = {
    // The measurement constructor handles splitting the input lines.
    NabMeasurement(
      currentStream,
      new String(bytes.slice(offset, offset + numBytes))
    )
  }
}
