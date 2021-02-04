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

package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.connectors.postgres.SquerylEntrypoint._
import nz.net.wand.streamevmon.measurements.amp._

import org.squeryl.{Schema, Table}

/** Defines the database schema of the PostgreSQL connection. Should be used in
  * conjunction with [[SquerylEntrypoint]].
  */
object PostgresSchema extends Schema {
  val icmpMeta: Table[ICMPMeta] = table("streams_amp_icmp")
  val dnsMeta: Table[DNSMeta] = table("streams_amp_dns")
  val tracerouteMeta: Table[TracerouteMeta] = table("streams_amp_traceroute")
  val tcppingMeta: Table[TCPPingMeta] = table("streams_amp_tcpping")
  val httpMeta: Table[HTTPMeta] = table("streams_amp_http")

  def traceroute(stream: Int): Table[Traceroute] = table(s"data_amp_traceroute_$stream")

  def traceroutePath(path_id: Int): Table[TraceroutePath] = table(s"data_amp_traceroute_paths_$path_id")

  def tracerouteAsPath(aspath_id: Int): Table[TracerouteAsPath] = table(s"data_amp_traceroute_aspaths_$aspath_id")
}
