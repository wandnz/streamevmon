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

package nz.net.wand.streamevmon.measurements.nab

import nz.net.wand.streamevmon.measurements.traits.{CsvOutputable, HasDefault, Measurement}

import java.time._
import java.time.format.DateTimeFormatter

/** Represents a measurement from the NAB dataset. Just a time and a value.
  * The stream corresponds to the filename the measurement is from.
  *
  * @see [[https://github.com/numenta/NAB]]
  */
case class NabMeasurement(
  stream: String,
  value : Double,
  time  : Instant
)
  extends Measurement
          with CsvOutputable
          with HasDefault {

  override def isLossy: Boolean = false

  override var defaultValue: Option[Double] = Some(value)

  override def toCsvFormat: Seq[String] = Seq(NabMeasurement.formatter.format(time), value.toString)

  override def toString: String = toCsvFormat.mkString(",")
}

object NabMeasurement {
  lazy val formatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("uuuu-MM-dd HH:mm:ss")
    .withZone(ZoneId.of("UTC"))

  def apply(stream: String, line: String): NabMeasurement = {
    val parts = line.split(',')

    if (parts.length != 2) {
      throw new IllegalArgumentException(s"NAB measurement did not have two columns! Stream ID: $stream, line: $line")
    }

    NabMeasurement(
      stream,
      parts(1).toDouble,
      LocalDateTime.parse(parts(0), formatter)
        .atZone(ZoneOffset.UTC)
        .toInstant
    )
  }
}
