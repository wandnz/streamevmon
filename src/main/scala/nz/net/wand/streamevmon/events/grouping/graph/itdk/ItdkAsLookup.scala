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

package nz.net.wand.streamevmon.events.grouping.graph.itdk

import nz.net.wand.streamevmon.Logging

import java.io.{File, RandomAccessFile}

import scala.annotation.tailrec

/** Reads the geolocation annotations for an ITDK dataset. Provides a mapping
  * from ITDK-sourced host IDs to their [[GeoInfo]].
  */
class ItdkAsLookup(asFile: File) extends Logging {
  @transient protected val reader = new RandomAccessFile(asFile, "r")

  @transient protected val fileSize: Long = asFile.length

  /** The longest line in the ITDK-2019-04 dataset is observed to be
    * 127 characters, though the file format specification mentions that it
    * could be up to around 300 characters.
    *
    * We'll read a few lines worth so we can seek a little back and forth
    * around the expected line. Benchmarking showed that increasing the
    * buffer size harms performance, though it's not really a noticeable
    * change until you start reaching the MiB ranges.
    */
  @transient protected val bufSize: Int = 4096

  /** Returns however many AsNumber entries we got, fully intact, from reading
    * into our buffer size. This is usually somewhere between 2 and 7 elements.
    */
  protected def getEntriesAfterLocation(idx: Long): Iterable[ItdkAsNumber] = {
    reader.seek(idx)
    val buf = Array.ofDim[Byte](bufSize)
    reader.readFully(buf, 0, math.min(bufSize, fileSize - idx).toInt)

    // Using the -1 ensures that any trailing entries still appear in the array.
    // This just makes the logic a little more consistent, especially at the
    // end of the file.
    val entries = new String(buf).split("\n", -1)
    val qualifiedEntries = entries
      .filter(_.startsWith("node.AS N"))
      .filter(e => ItdkAsMethod.values.exists(method => e.endsWith(method.toString)))
    qualifiedEntries.map(ItdkAsNumber(_))
  }

  /** Recursively searches the data file for the given node ID.
    *
    * @see [[ItdkAliasLookup.search]], which behaves the same as this function.
    *      The only difference is that each file lookup here can retrieve
    *      multiple entries.
    */
  @tailrec
  protected final def search(
    lowGuess : Long,
    highGuess: Long,
    target   : Long,
    lastGuess: Option[Long] = None,
    depth    : Int = 1
  ): Option[ItdkAsNumber] = {
    // We do a simple binary chop.
    val midpointOfGuesses = math.max((highGuess + lowGuess) / 2, 0)
    val results = getEntriesAfterLocation(midpointOfGuesses)

    // This shouldn't really happen, but we do need to check for it.
    if (results.isEmpty) {
      logger.trace(s"No results obtained for target $target from guess $midpointOfGuesses because we got the same result twice")
      None
    }
    // If we get the same result twice, then we know we haven't found the target
    // and that nothing will change from here, since the binary chop moves less
    // and less each iteration.
    else if (lastGuess.isDefined && lastGuess.get == midpointOfGuesses) {
      logger.trace(s"Failed to find target $target after $depth lookups")
      None
    }
    else {
      results.find(_.nodeId == target) match {
        case Some(value) =>
          logger.trace(s"Found target $target after $depth guesses")
          Some(value)
        case None =>
          // If we retrieved results that are both higher and lower than the
          // target in the same go, then we know the target isn't there.
          if (results.head.nodeId < target && results.last.nodeId > target) {
            logger.trace(s"Failed to find target $target after $depth lookups because it is not present")
            None
          }
          else {
            if (results.head.nodeId > target) {
              search(lowGuess, midpointOfGuesses, target, Some(midpointOfGuesses), depth + 1)
            }
            else {
              search(midpointOfGuesses, highGuess, target, Some(midpointOfGuesses), depth + 1)
            }
          }
      }
    }
  }

  /** Attempts to retrieve an ItdkAsNumber from a node ID. */
  def getAsNumberByNode(nodeId: Int): Option[ItdkAsNumber] = {
    search(0, fileSize, nodeId)
  }
}
