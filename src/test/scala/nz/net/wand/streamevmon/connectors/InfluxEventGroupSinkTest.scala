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

import nz.net.wand.streamevmon.events.grouping.EventGroup
import nz.net.wand.streamevmon.flink.MockSinkContext
import nz.net.wand.streamevmon.flink.sinks.InfluxEventGroupSink
import nz.net.wand.streamevmon.test.{InfluxContainerSpec, SeedData}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.typelevel.jawn.ast.JNull

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => ScalaDuration}

class InfluxEventGroupSinkTest extends InfluxContainerSpec {
  "InfluxEventGroupSink" should {
    "write data to InfluxDB" in {
      val sink = setupSinkFunction(new InfluxEventGroupSink, "no-subscription")
      sink.open(null)

      val groups = Seq(
        SeedData.eventgroup.oneEvent,
        SeedData.eventgroup.multipleEvents,
        SeedData.eventgroup.oneEventWithNoEndTime
      )
      groups.foreach(g => sink.invoke(g, new MockSinkContext(g.startTime)))
      sink.close()

      val db = InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
        .database(container.database)

      Await.result(
        db.readJson(
          s"SELECT time,endTime,modeEventType,meanSeverity,meanDetectionLatency FROM ${EventGroup.getMeasurementName(groups.head)}"
        )
          .map {
            case Left(err) => fail(err)
            case Right(items) =>
              items.zip(groups).foreach { case (got, expected) =>
                Instant.parse(got.get(0).asString) shouldBe expected.startTime.truncatedTo(ChronoUnit.MILLIS)

                got.get(1) match {
                  case _@JNull => expected.endTime shouldBe None
                  case _ => Instant.ofEpochMilli(
                    TimeUnit.NANOSECONDS.toMillis(
                      got.get(1).asString.dropRight(1).toLong
                    )
                  ) shouldBe expected.endTime.get.truncatedTo(ChronoUnit.MILLIS)
                }

                expected.events.map(_.eventType) should contain(got.get(2).asString)

                got.get(3).asInt shouldBe expected.events.head.severity

                got.get(4).asInt shouldBe expected.events.head.detectionLatency.toNanos
              }
          },
        ScalaDuration.Inf
      )
    }
  }
}
