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

package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat

import org.apache.flink.api.scala.operators.ScalaCsvOutputFormat
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.scala._

/** Simple entrypoint which parses the Latency TS I dataset, converts it to the CSV
  * representation of our internal [[nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP LatencyTSAmpICMP]]
  * object, and outputs it to both the terminal and a new CSV file.
  *
  * @see [[https://wand.net.nz/wits/latency/1/]]
  */
object LatencyTSToCsvPrinter {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    val input = new LatencyTSAmpFileInputFormat
    val output: ScalaCsvOutputFormat[(String, String, String, String, String, String, String)] =
      new ScalaCsvOutputFormat(new Path("out/output.csv"))
    output.setWriteMode(WriteMode.OVERWRITE)

    val tupleStream = env
      // We just read one file for simplicity here. You can specify a folder to
      // read all the files in that folder, if you choose.
      .readFile(input, "data/latency-ts-i/ampicmp/waikato-xero-ipv4.series")
      .map(_.toCsvFormat match {
        case Seq(a, b, c, d, e, f, g) => (a, b, c, d, e, f, g)
      })

    tupleStream.writeUsingOutputFormat(output)
    tupleStream.print()

    env.execute()
  }
}
