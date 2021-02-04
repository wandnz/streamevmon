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

/** This package contains traits that Measurements and related objects may
  * implement. These provide additional functionality, and many detectors
  * require that input measurements implement one or more of these traits.
  *
  * Particularly notable entries include the following:
  *
  * - `Measurement` must be implemented by all measurements.
  * - `HasDefault` is required by many detectors, as it simplifies delivery of
  * a simple time series by allowing measurements to offer a simple default
  * Double value.
  * - `InfluxMeasurement` and `PostgresMeasurement` define certain other traits
  * that are common to all measurements stored in particular databases
  * (particularly in the context of AMP).
  * - `MeasurementMeta` represents metadata about a measurement stream.
  */
package object traits {}
