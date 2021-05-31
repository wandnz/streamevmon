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

package nz.net.wand.streamevmon.connectors.influx

import nz.net.wand.streamevmon.measurements.amp2.Direction

import java.time.Instant
import java.util.concurrent.TimeUnit

import scala.annotation.tailrec
import scala.util.control.Breaks.{break, breakable}
import scala.util.Try

case class LineProtocol(
  measurementName: String,
  tags           : Map[String, String],
  fields         : Map[String, String],
  time           : Instant
) {
  def getTagAsLong(key: String): Long = {
    tags.get(key).flatMap(v => Try(v.toLong).toOption)
      .getOrElse(throw new IllegalArgumentException(
        s"Could not convert tag $key with value ${tags.get(key)} to Int"
      ))
  }

  def getTagAsBoolean(key: String): Boolean = {
    tags.get(key).flatMap(v => Try(v.toBoolean).toOption)
      .getOrElse(throw new IllegalArgumentException(
        s"Could not convert tag $key with value ${tags.get(key)} to Boolean"
      ))
  }

  def getTagAsDirection(key: String): Direction = {
    tags.get(key).map(v => Direction(v))
      .getOrElse(throw new IllegalArgumentException(
        s"""Could not convert tag $key with value ${tags.get(key)} to Direction - expected \"in\" or \"out\""""
      ))
  }

  def getFieldAsLong(key: String): Option[Long] = {
    fields.get(key).map(_.dropRight(1).toLong)
  }

  def getFieldAsDouble(key: String): Option[Double] = {
    fields.get(key).map(_.toDouble)
  }
}

object LineProtocol {
  /** Like string.split(), but it ignores separators that are inside double quotes.
    *
    * @param line              The line to split.
    * @param precedingElements The newly discovered elements are appended to this.
    * @param separators        The separators to split on.
    */
  @tailrec
  final protected def splitLineProtocol(
    line             : String,
    precedingElements: Seq[String] = Seq(),
    separators       : Seq[Char] = Seq(',', ' ')
  ): Seq[String] = {
    var splitPoint = -1
    var quoteCount = 0
    // Iterate through the string, looking for separators.
    // Separators that are in quotes don't count, since they're part of a value.
    // We stop when we find the first one.
    breakable {
      for (i <- Range(0, line.length)) {
        if (line(i) == '"') {
          quoteCount += 1
        }
        if (quoteCount % 2 == 0 && separators.contains(line(i))) {
          splitPoint = i
          break
        }
      }
    }

    // If there aren't any left, we'll complete the seq and return it.
    if (splitPoint == -1) {
      precedingElements :+ line
    }
    // Otherwise, we'll split around the separator and give the rest to the
    // recursive function.
    else {
      val beforeSplit = line.substring(0, splitPoint)
      val afterSplit = line.substring(splitPoint + 1)
      splitLineProtocol(afterSplit, precedingElements :+ beforeSplit, separators)
    }
  }

  protected def entryToTuple(entry: String): Option[(String, String)] = {
    val parts = entry.split('=')
    if (parts.length != 2) {
      None
    }
    else {
      Some((parts.head, parts.drop(1).head))
    }
  }

  def apply(line: String): Option[LineProtocol] = {
    val splitTypes = splitLineProtocol(line, separators = " ")
    if (splitTypes.size != 3) {
      None
    }
    else {
      val tags = splitLineProtocol(splitTypes.head, separators = ",")
      val fields = splitLineProtocol(splitTypes.drop(1).head, separators = ",")
      val time = splitTypes.last

      val theTags = tags.drop(1).map(entryToTuple)
      val theFields = fields.map(entryToTuple)

      if (theTags.contains(None) || theFields.contains(None)) {
        None
      }
      else {
        Some(LineProtocol(
          tags.head,
          theTags.flatten.toMap,
          theFields.flatten.toMap,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(time.toLong))
        ))
      }
    }
  }
}
