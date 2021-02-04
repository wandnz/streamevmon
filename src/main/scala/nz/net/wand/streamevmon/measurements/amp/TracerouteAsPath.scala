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

package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.connectors.postgres.schema.AsPath

/** An [[nz.net.wand.streamevmon.connectors.postgres.schema.AsPath AsPath]], with
  * some extra metadata. This represents the table named
  * `data_amp_traceroute_aspaths_{aspath_id}` in PostgreSQL.
  *
  * `aspath_length` should be the same as the sum of the `hopsInAs` fields of
  * the elements of `aspath`.
  *
  * `uniqueas` should be the same as the length of `aspath`.
  *
  * `responses` should be the same as the sum of the `hopsInAs` fields of those
  * elements of `aspath` that are not in the `Missing`
  * [[nz.net.wand.streamevmon.connectors.postgres.schema.AsNumberCategory AsNumberCategory]].
  */
case class TracerouteAsPath(
  aspath_id: Int,
  aspath_length: Int,
  uniqueas: Int,
  responses: Int,
  aspath: AsPath
)
