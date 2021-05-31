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

import java.time.Instant

case class Throughput(
  source: String,
  destination: String,
  test: String,
  time: Instant,
  direction: Direction,
  dscp: String,
  family: String,
  protocol: String,
  write_size: Int,
  bytes: Option[Int],
  count: Option[Int],
  duration: Option[String],
  runtime: Option[Float]
) extends Amp2Measurement {
  override val measurementName: String = Throughput.measurementName
  override val tags: Seq[Any] = Seq(direction, dscp, family, protocol, write_size)

  override var defaultValue: Option[Double] = bytes.map(_.toDouble)

  lazy val bytesPerRuntime: Option[Double] = bytes.flatMap { b =>
    runtime.map { r =>
      b.toDouble / r
    }
  }
}

object Throughput {

  val measurementName = "throughput"

  def createFromLineProtocol(line: String): Option[Throughput] = ???
}
