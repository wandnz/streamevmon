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

/** This package contains representations of data from the
  * [[https://wand.net.nz/wits/latency/1/ Latency TS I]] dataset. While part of
  * the dataset is AMP ICMP measurements, there have been some changes in the
  * format of ICMP measurements since this dataset was gathered in 2014 and the
  * present. As such, we need a unique representation.
  *
  * Both of these types inherit from
  * [[nz.net.wand.streamevmon.measurements.traits.RichMeasurement RichMeasurement]],
  * [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]], and
  * [[nz.net.wand.streamevmon.measurements.traits.CsvOutputable CsvOutputable]],
  * meaning they can be used in a wide variety of detectors.
  *
  * Use the [[nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * and [[nz.net.wand.streamevmon.flink.sources.LatencyTSSmokepingFileInputFormat LatencyTSSmokepingFileInputFormat]]
  * functions to ingest this dataset, as shown in [[nz.net.wand.streamevmon.runners.examples.LatencyTSToCsvPrinter LatencyTSToCsvPrinter]].
  */
package object latencyts {}
