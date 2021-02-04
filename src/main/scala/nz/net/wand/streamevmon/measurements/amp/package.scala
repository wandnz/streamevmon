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

/** This package represents data gathered by [[https://amp.wand.net.nz/ AMP]],
  * WAND's Active Measurement Project. It should be a comprehensive
  * representation of the measurements as they are stored in InfluxDB.
  *
  * As such, all AMP measurements are [[nz.net.wand.streamevmon.measurements.traits.InfluxMeasurement InfluxMeasurements]].
  * AMP measurements can be enriched into [[nz.net.wand.streamevmon.measurements.traits.RichInfluxMeasurement RichInfluxMeasurements]]
  * using [[nz.net.wand.streamevmon.measurements.traits.InfluxMeasurementFactory.enrichMeasurement InfluxMeasurementFactory.enrichMeasurement]].
  *
  * The metadata used to enrich a measurement is obtained from PostgreSQL (see
  * [[nz.net.wand.streamevmon.connectors.postgres Postgres connector package]]),
  * and can be obtained separately as a
  * [[nz.net.wand.streamevmon.measurements.traits.PostgresMeasurementMeta MeasurementMeta]].
  * The metadata is information about the test schedule which produced the
  * measurement.
  *
  */
package object amp {}
