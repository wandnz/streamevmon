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

package nz.net.wand.streamevmon.connectors

/** This package contains an interface to PostgreSQL that produces
  * [[nz.net.wand.streamevmon.measurements.traits.PostgresMeasurementMeta MeasurementMeta]]
  * objects. Create a
  * [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection PostgresConnection]],
  * and call its [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection.getMeta getMeta]] function.
  *
  * ==Configuration==
  *
  * This module is configured by the `source.postgres` config key group.
  *
  * - `serverName`: The hostname which the PostgreSQL database can be reached on.
  * Default: "localhost"
  *
  * - `portNumber`: The port that PostgreSQL is running on.
  * Default: 5432
  *
  * - `databaseName`: The database which the metadata is stored in.
  * Default: "nntsc"
  *
  * - `user`: The username which should be used to connect to the database.
  * Default: "cuz"
  *
  * - `password`: The password which should be used to connect to the database.
  * Default: ""
  *
  * This module uses [[nz.net.wand.streamevmon.Caching Caching]]. The TTL value
  * of the cached results can be set with the `caching.ttl` configuration key,
  * which defaults to 30 seconds.
  */
package object postgres {}
