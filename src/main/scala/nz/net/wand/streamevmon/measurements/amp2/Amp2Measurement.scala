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

package nz.net.wand.streamevmon.measurements.amp2

import nz.net.wand.streamevmon.measurements.traits.{HasDefault, Measurement}

trait Amp2Measurement extends Measurement with HasDefault {
  val source: String
  val destination: String
  val test: String

  val measurementName: String

  /** The unique tags for the measurement type. Do not include `source`,
    * `destination`, `test`, or `measurementName`. When combined with those four
    * fields, the contents of this field should create a unique identifier for
    * a measurement stream.
    */
  val tags: Seq[Any]

  override lazy val stream: String = (
    Seq(measurementName, source, destination, test) ++
      tags.map(_.toString)
    ).mkString("-")

  override def isLossy: Boolean = defaultValue.isEmpty
}

object Amp2Measurement {
  def createFromLineProtocol(line: String): Option[Amp2Measurement] = {
    line.substring(0, line.indexOf(',')) match {
      case Http.measurementName => Http.createFromLineProtocol(line: String)
      case Latency.measurementName => Latency.createFromLineProtocol(line: String)
      case Pathlen.measurementName => Pathlen.createFromLineProtocol(line: String)
      case Traceroute.measurementName => Traceroute.createFromLineProtocol(line: String)
      case _ => None
    }
  }
}
