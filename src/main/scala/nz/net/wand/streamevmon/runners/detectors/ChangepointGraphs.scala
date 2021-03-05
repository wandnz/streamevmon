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

package nz.net.wand.streamevmon.runners.detectors

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat
import nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import java.io.File

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}

/** This class is an alternative runner for the changepoint detector that allows
  * iteration over configuration changes and various input files. It also has
  * the graphing options turned on, meaning some .csv files will be output into
  * ./out/graphs
  *
  * @see [[ChangepointRunner]]
  */
object ChangepointGraphs {

  def doIt(file: String, maxhist: Int, triggerCount: Int, severity: Int): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val conf = Array(
      "--detector.changepoint.maxHistory",
      s"$maxhist",
      "--detector.changepoint.triggerCount",
      s"$triggerCount",
      "--detector.changepoint.severityThreshold",
      s"$severity"
    )

    env.getConfig.setGlobalJobParameters(Configuration.get(conf))

    env.disableOperatorChaining

    val source = env
      .readFile(
        new LatencyTSAmpFileInputFormat,
        file
      )
      .name("Latency TS I AMP ICMP Parser")
      .setParallelism(1)
      .filter(_.lossrate == 0.0)
      .keyBy(new MeasurementKeySelector[LatencyTSAmpICMP])

    implicit val ti: TypeInformation[NormalDistribution[LatencyTSAmpICMP]] = TypeInformation.of(classOf[NormalDistribution[LatencyTSAmpICMP]])

    val detector =
      new ChangepointDetector[LatencyTSAmpICMP, NormalDistribution[LatencyTSAmpICMP]](
        new NormalDistribution[LatencyTSAmpICMP](mean = 0),
        shouldDoGraphs = true,
        Some(file)
      )

    val process = source
      .process(detector)
      .name(detector.flinkName)

    process.print(s"${getClass.getSimpleName} sink")

    env.execute("Latency TS I AMP ICMP -> Changepoint Detector")
  }

  def getListOfFiles(dir: String): Seq[String] = {
    val file = new File(dir)
    file.listFiles
      .filter(_.isFile)
      .map(_.getPath)
      .toList
  }

  def main(args: Array[String]): Unit = {
    for (file <- getListOfFiles("data/latency-ts-i/ampicmp")) {
      if (file.endsWith(".series")) {
        doIt(file, 20, 10, 30)

        /*
        for (maxhist <- Seq(20, 40, 60)) {
          for (triggerCount <- Seq(10, 20, 30, 40)) {
            for (severity <- Seq(15, 20, 25, 30)) {
              doIt(file, maxhist, triggerCount, severity)
            }
          }
        }

       */
      }
    }
  }
}
