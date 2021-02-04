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

package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.Logging

/** Translates the event type string from an API response to an enum value
  * corresponding to a schema.TimeSeriesEntry type. Mainly used by
  * [[EsmondConnectionForeground]], which includes a function to map these
  * types to constructors.
  */
object ResponseType extends Enumeration with Logging {
  type ResponseType = Value
  val Failure: Value = Value
  val Histogram: Value = Value
  val Href: Value = Value
  val PacketTrace: Value = Value
  val Simple: Value = Value
  val Subintervals: Value = Value

  def fromString(eventType: String): ResponseType = eventType.toLowerCase match {
    case "failures" => Failure
    case "histogram-ttl" | "histogram-owdelay" => Histogram
    case "pscheduler-run-href" => Href
    case "packet-trace" => PacketTrace
    case "time-error-estimates" |
         "packet-duplicates" |
         "packet-loss-rate" |
         "packet-count-sent" |
         "packet-count-lost" |
         "throughput" |
         "packet-retransmits" |
         "packet-reorders"
    => Simple
    case "throughput-subintervals" | "packet-retransmits-subintervals" => Subintervals
    case "path-mtu" => logger.error("Found path-mtu data!"); Simple
    case other: String => throw new IllegalArgumentException(s"Unknown response type $other")
  }
}
