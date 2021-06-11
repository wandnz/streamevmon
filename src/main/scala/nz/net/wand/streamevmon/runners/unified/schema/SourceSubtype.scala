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

package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sources._
import nz.net.wand.streamevmon.measurements.traits.Measurement

import org.apache.flink.api.common.io.FileInputFormat
import org.apache.flink.streaming.api.functions.source.SourceFunction

/** This enum includes logic to build subtypes of sources. We must have subtypes
  * for all types within this enum, since we can't make an inheritance tree.
  */
object SourceSubtype extends Enumeration {

  val Amp: ValueBuilder = new ValueBuilder("amp")
  val Amp2: ValueBuilder = new ValueBuilder("amp2")
  val Bigdata: ValueBuilder = new ValueBuilder("bigdata")
  val LatencyTSAmp: ValueBuilder = new ValueBuilder("ampicmp")
  val LatencyTSSmokeping: ValueBuilder = new ValueBuilder("smokeping")

  class ValueBuilder(name: String) extends Val(name) {
    def buildSourceFunction(): SourceFunction[Measurement] with HasFlinkConfig = {
      val built = this match {
        case Amp => new AmpMeasurementSourceFunction()
        case Amp2 => new Amp2SourceFunction()
        case Bigdata => new BigDataSourceFunction()
        case _ => throw new UnsupportedOperationException(s"Source subtype $this is not a SourceFunction")
      }
      built.asInstanceOf[SourceFunction[Measurement] with HasFlinkConfig]
    }

    def buildFileInputFormat(): FileInputFormat[Measurement] with HasFlinkConfig = {
      val built = this match {
        case LatencyTSAmp => new LatencyTSAmpFileInputFormat()
        case LatencyTSSmokeping => new LatencyTSSmokepingFileInputFormat()
        case _ => throw new UnsupportedOperationException(s"Source subtype $this is not a FileInputFormat")
      }
      built.asInstanceOf[FileInputFormat[Measurement] with HasFlinkConfig]
    }
  }
}
