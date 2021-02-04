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

package nz.net.wand.streamevmon.connectors.esmond.schema

import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.io.Serializable

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonPropertyOrder}

/** An entry in a metadata archive's event-type field might contain some of these.
  *
  * @see [[EsmondAPI.archive]]
  */
@JsonPropertyOrder(alphabetic = true)
class Summary extends Serializable {
  // Unfortunately, this field is usually different to the part of the URI that
  // it corresponds to. We'll have to expose a field which bypasses the
  // discrepancies, instead of the raw field.
  // It would be tidier and safer to use an enum for this, since there are only
  // three possibilities at the time of writing. However, it could be expanded
  // later and it's more work for not much benefit.
  @JsonProperty("summary-type")
  protected val summaryTypeRaw: String = ""
  @JsonIgnore
  lazy val summaryType: String = {
    if (Summary.summaryTypeOverrides.contains(summaryTypeRaw)) {
      Summary.summaryTypeOverrides(summaryTypeRaw)
    }
    else {
      summaryTypeRaw
    }
  }

  @JsonProperty("summary-window")
  val summaryWindow: Long = Long.MinValue

  @JsonProperty("time-updated")
  val timeUpdated: Long = Long.MinValue

  @JsonProperty("uri")
  val uri: String = ""

  // These fields could probably be obtained more elegantly, but it does work
  // for getting the fields which are otherwise missing.
  @JsonIgnore
  lazy val metadataKey: String = uri.split('/')(4)

  @JsonIgnore
  lazy val eventType: String = uri.split('/')(5)

  override def toString: String = uri

  def canEqual(other: Any): Boolean = other.isInstanceOf[Summary]

  override def equals(other: Any): Boolean = other match {
    case that: Summary =>
      (that canEqual this) &&
        summaryType == that.summaryType &&
        summaryWindow == that.summaryWindow &&
        timeUpdated == that.timeUpdated &&
        uri == that.uri &&
        metadataKey == that.metadataKey &&
        eventType == that.eventType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(summaryType, summaryWindow, timeUpdated, uri, metadataKey, eventType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Summary {
  protected val summaryTypeOverrides = Map(
    "aggregation" -> "aggregations",
    "average" -> "averages"
  )
}
