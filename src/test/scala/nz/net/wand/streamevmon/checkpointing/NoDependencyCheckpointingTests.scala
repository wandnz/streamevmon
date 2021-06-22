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

package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.test.{HarnessingTest, SeedData}

import java.time.Instant

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness

class NoDependencyCheckpointingTests extends HarnessingTest {
  protected def sendNormalMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times  : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 1000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 1000)),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  protected def sendAnomalousMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times  : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          e.loss,
          e.lossrate,
          e.median.map(_ * 100000),
          e.packet_size,
          e.results,
          e.rtts.map(_.map(_ * 100000)),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  protected def sendLossyMeasurement[O](
    harness: KeyedOneInputStreamOperatorTestHarness[String, ICMP, O],
    times                                   : Int
  ): Unit = {
    val e = SeedData.icmp.expected
    for (_ <- Range(0, times)) {
      currentTime += 1
      harness.processElement(
        ICMP(
          e.stream,
          Some(1),
          Some(1.0),
          None,
          e.packet_size,
          Some(1),
          Seq(None),
          Instant.ofEpochMilli(currentTime)
        ),
        currentTime
      )
    }
  }

  "Detectors with no external dependencies" should {
    "restore from checkpoints correctly" when {
      "type is BaselineDetector" in {
        def detector: BaselineDetector[ICMP] = new BaselineDetector[ICMP]

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is ChangepointDetector" in {
        implicit val ti: TypeInformation[NormalDistribution[ICMP]] =
          TypeInformation.of(classOf[NormalDistribution[ICMP]])

        def detector: ChangepointDetector[ICMP, NormalDistribution[ICMP]] =
          new ChangepointDetector[ICMP, NormalDistribution[ICMP]](
            new NormalDistribution[ICMP](mean = 0)
          )

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is DistDiffDetector" in {
        def detector: DistDiffDetector[ICMP] = new DistDiffDetector[ICMP]

        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is LossDetector" in {
        def detector: LossDetector[ICMP] = new LossDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 30)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendLossyMeasurement(harness, times = 30)
        harness.getOutput shouldNot have size 0
      }

      "type is ModeDetector" in {
        def detector: ModeDetector[ICMP] = new ModeDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 120)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 120)
        harness.getOutput shouldNot have size 0
      }

      "type is SpikeDetector" in {
        def detector: SpikeDetector[ICMP] = new SpikeDetector[ICMP]
        var harness = newHarness(detector)
        harness.open()

        sendNormalMeasurement(harness, times = 100)
        harness.getOutput should have size 0

        harness = snapshotAndRestart(harness, detector)

        sendAnomalousMeasurement(harness, times = 100)
        harness.getOutput shouldNot have size 0
      }
    }
  }
}
