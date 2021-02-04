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

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyOrder}

@JsonPropertyOrder(alphabetic = true)
class PacketTraceTimeSeriesEntry extends AbstractTimeSeriesEntry {
  @JsonProperty("val")
  val value: Iterable[PacketTraceEntry] = Seq()

  def canEqual(other: Any): Boolean = other.isInstanceOf[PacketTraceTimeSeriesEntry]

  override def equals(other: Any): Boolean = other match {
    case that: PacketTraceTimeSeriesEntry =>
      (that canEqual this) &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

@JsonPropertyOrder(alphabetic = true)
class ASEntry extends Serializable {
  @JsonProperty("owner")
  val owner: String = null
  @JsonProperty("number")
  val number: Int = Int.MinValue

  def canEqual(other: Any): Boolean = other.isInstanceOf[ASEntry]

  override def equals(other: Any): Boolean = other match {
    case that: ASEntry =>
      (that canEqual this) &&
        owner == that.owner &&
        number == that.number
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(owner, number)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

@JsonPropertyOrder(alphabetic = true)
class PacketTraceEntry extends Serializable {
  @JsonProperty("success")
  val success: Int = Int.MinValue
  @JsonProperty("ip")
  val ip: Option[String] = None
  @JsonProperty("hostname")
  val hostname: Option[String] = None
  @JsonProperty("rtt")
  val rtt: Option[Double] = None
  @JsonProperty("as")
  val as: Option[ASEntry] = None
  @JsonProperty("ttl")
  val ttl: Int = Int.MinValue
  @JsonProperty("query")
  val query: Int = Int.MinValue
  @JsonProperty("error-message")
  val errorMessage: Option[String] = None

  def canEqual(other: Any): Boolean = other.isInstanceOf[PacketTraceEntry]

  override def equals(other: Any): Boolean = other match {
    case that: PacketTraceEntry =>
      (that canEqual this) &&
        success == that.success &&
        ip == that.ip &&
        hostname == that.hostname &&
        rtt == that.rtt &&
        as == that.as &&
        ttl == that.ttl &&
        query == that.query
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(success, ip, hostname, rtt, as, ttl, query)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
