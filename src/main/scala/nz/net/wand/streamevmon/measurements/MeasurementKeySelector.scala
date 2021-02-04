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

import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata._
import nz.net.wand.streamevmon.measurements.esmond._
import nz.net.wand.streamevmon.measurements.latencyts._
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement
import nz.net.wand.streamevmon.measurements.traits.Measurement

import org.apache.flink.api.java.functions.KeySelector

import scala.reflect.ClassTag

/** Used to convert a DataStream to a KeyedStream. Stream ID is based on the
  * type and `stream` field of the measurement. This means that items sharing a
  * value for `stream`, but with a different concrete type will usually not be
  * sent to the same downstream operators.
  */
class MeasurementKeySelector[T <: Measurement : ClassTag] extends KeySelector[T, String] {
  override def getKey(value: T): String =
    value match {
      case m@(_: DNS | _: RichDNS) => s"DNS-${m.stream}"
      case m@(_: HTTP | _: RichHTTP) => s"HTTP-${m.stream}"
      case m@(_: ICMP | _: RichICMP) => s"ICMP-${m.stream}"
      case m@(_: TCPPing | _: RichTCPPing) => s"TCPPing-${m.stream}"
      case m@(_: TraceroutePathlen | _: RichTraceroutePathlen) => s"TraceroutePathlen-${m.stream}"
      case m@(_: LatencyTSAmpICMP) => s"LatencyTSAmpICMP-${m.stream}"
      case m@(_: LatencyTSSmokeping) => s"LatencyTSSmokeping-${m.stream}"
      case m@(_: Flow) => s"Flow-${m.stream}"
      case m@(_: EsmondMeasurement) => s"esmond-${m.stream}"
      case m@(_: NabMeasurement) => s"nab-${m.stream}"
      case m => throw new IllegalArgumentException(s"Unknown measurement type ${m.getClass.getSimpleName}")
    }
}
