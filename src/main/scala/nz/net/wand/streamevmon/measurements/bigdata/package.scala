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

package nz.net.wand.streamevmon.measurements

/** This package contains representations of the output of the
  * [[https://github.com/jacobvw/libtrace-bigdata Libtrace-Bigdata]] packet
  * analysis tool, created by Jacob van Walraven. The data can be gathered from
  * InfluxDB either as soon as it is inserted (via an InfluxSourceFunction) or
  * historical data can be queried.
  *
  * Currently, only the flow_statistics table is supported, and is represented
  * by the [[nz.net.wand.streamevmon.measurements.bigdata.Flow Flow]] class.
  * While it has many fields, most are optional. The source and destination
  * location fields are in order to support the output of the Maxmind plugin,
  * which gives geolocation for IP addresses. These can also be accessed via the
  * [[nz.net.wand.streamevmon.measurements.bigdata.Flow.Endpoint Endpoint]]
  * objects named `source` and `destination`.
  */
package object bigdata {}
