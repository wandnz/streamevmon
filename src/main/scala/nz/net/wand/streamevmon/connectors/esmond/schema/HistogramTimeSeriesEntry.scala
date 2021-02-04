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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class HistogramTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  private val valueInternal: Map[String, Int] = Map()
  // We need this shim, because Retrofit/Jackson isn't smart enough to correctly
  // cast the type at runtime, which results in what looks like a Double, but
  // is actually a String. This causes ClassCastExceptions when it tries to
  // access the value as a Double.
  // Buckets are always in milliseconds.
  @JsonIgnore
  lazy val value: Map[Double, Int] = valueInternal.map(v => (v._1.toDouble, v._2))

  override def toString: String = s"histogram at time ${timestamp.toString}"

  def canEqual(other: Any): Boolean = other.isInstanceOf[HistogramTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: HistogramTimeSeriesEntry =>
      (that canEqual this) &&
        valueInternal == that.valueInternal &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(valueInternal, value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
