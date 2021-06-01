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
import scala.util.Try
import scala.util.control.Breaks.{break, breakable}

/** This class represents a single measurement in InfluxDB Line Protocol format.
  * It allows users to convert tags and fields to particular types, and provides
  * a simple interface to the data.
  *
  * All numeric datatypes in InfluxDB are 64-bit, so the names don't match up
  * with Scala type names. This class uses Scala type names, so a database entry
  * with type integer is a Scala Long, and a float is a Scala Double.
  */
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
        s"""Could not convert tag $key with value ${tags.get(key)} to Direction - expected "in" or "out""""
      ))
  }

  def getFieldAsLong(key: String): Option[Long] = {
    fields.get(key).map(_.dropRight(1).toLong)
  }

  def getFieldAsDouble(key: String): Option[Double] = {
    fields.get(key).map(_.toDouble)
  }
}

/** Contains the apply method to convert from a line protocol string to a
  * LineProtocol, as well as some helper functions.
  */
object LineProtocol {
  /** Tail-recursive implementation for `splitUnlessQuoted`. This shouldn't be
    * called directly.
    */
  @tailrec
  final protected def splitUnlessQuotedImpl(
    unprocessedLine: Seq[Char],
    quoteCount     : Int = 0,
    lineSinceLastSplit: Seq[Char] = Seq(),
    alreadySplitItems: Seq[String] = Seq(),
    separators: Set[Char] = Set(',', ' ')
  ): Seq[String] = {
    // If there's nothing left, return the parts we calculated
    if (unprocessedLine.isEmpty) {
      alreadySplitItems :+ lineSinceLastSplit.mkString
    }
    // If we've seen an uneven number of quotes, then a quoted value is ongoing,
    // and we shouldn't split at this character.
    // If there's an even number of quotes, and we find a separator character,
    // then we should split here.
    else if (quoteCount % 2 == 0 && separators.contains(unprocessedLine.head)) {
      splitUnlessQuotedImpl(
        unprocessedLine.drop(1),
        quoteCount,
        "",
        alreadySplitItems :+ lineSinceLastSplit.mkString,
        separators
      )
    }
    // If we're at a quote, note that we found another one.
    else if (unprocessedLine.head == '"') {
      splitUnlessQuotedImpl(
        unprocessedLine.drop(1),
        quoteCount + 1,
        lineSinceLastSplit :+ unprocessedLine.head,
        alreadySplitItems,
        separators
      )
    }
    // Otherwise, just keep going
    else {
      splitUnlessQuotedImpl(
        unprocessedLine.drop(1),
        quoteCount,
        lineSinceLastSplit :+ unprocessedLine.head,
        alreadySplitItems,
        separators
      )
    }
  }

  /** Like string.split(), but ignoring separators that are contained inside "".
    *
    * In my opinion, this code is more readable and nicer, but unfortunately it's
    * around 10x slower, so we're sticking with the splitLineProtocol solution.
    */
  final protected def splitUnlessQuoted(line: String, separators: Set[Char]): Seq[String] = {
    splitUnlessQuotedImpl(unprocessedLine = line, separators = separators)
  }

  @tailrec
  final protected def splitLineProtocol(
    line             : String,
    precedingElements: Seq[String] = Seq(),
    separators       : Set[Char] = Set(',', ' ')
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
    val splitTypes = splitLineProtocol(line, separators = Set(' '))
    if (splitTypes.size != 3) {
      None
    }
    else {
      val tags = splitLineProtocol(splitTypes.head, separators = Set(','))
      val fields = splitLineProtocol(splitTypes.drop(1).head, separators = Set(','))
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
