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

/** This package represents data from the ESnet perfSONAR esmond API.
  *
  * [[nz.net.wand.streamevmon.measurements.esmond.EsmondMeasurement EsmondMeasurement]]
  * is the top-level class, from which all other classes inherit. There is also
  * the [[nz.net.wand.streamevmon.measurements.esmond.RichEsmondMeasurement RichEsmondMeasurement]],
  * which classes that contain a bit of extra metadata inherit from.
  *
  * Each type of EsmondMeasurement represents one or more `eventType`s, depending
  * on the format of their `value` field. The `stream` field is derived directly
  * from `metadataKey`, which is unique for a particular data stream.
  *
  * Some EsmondMeasurements implement
  * [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]] and
  * [[nz.net.wand.streamevmon.measurements.traits.CsvOutputable CsvOutputable]],
  * but others have more complex value types and do not.
  *
  * To obtain EsmondMeasurements, you should use the
  * [[nz.net.wand.streamevmon.flink.sources.PollingEsmondSourceFunction PollingEsmondSourceFunction]],
  * which produces RichEsmondMeasurements.
  */
package object esmond {}
