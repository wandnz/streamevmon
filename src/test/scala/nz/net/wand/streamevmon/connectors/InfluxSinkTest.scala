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

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.MockSinkContext
import nz.net.wand.streamevmon.test.{InfluxContainerSpec, SeedData}

import java.time.{Instant, Duration => JavaDuration}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => ScalaDuration}

class InfluxSinkTest extends InfluxContainerSpec {
  "InfluxSink" should {
    "write data to InfluxDB" in {
      val sink = getSinkFunction("no-subscription")
      sink.open(null)

      sink.invoke(SeedData.event.withTags, new MockSinkContext(SeedData.event.withTags.time))
      sink.invoke(SeedData.event.withoutTags, new MockSinkContext(SeedData.event.withoutTags.time))

      sink.close()

      val db = InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
        .database(container.database)

      Await.result(
        db.readJson(
          s"SELECT time,stream,severity,detection_latency,description FROM ${SeedData.event.withoutTags.eventType}")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = SeedData.event.withoutTags.eventType,
              stream = arr.get(1).toString.drop(1).dropRight(1),
              severity = arr.get(2).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(3).asInt),
              description = arr.get(4).asString,
              tags = Map()
            ) shouldBe SeedData.event.withoutTags
          }),
        ScalaDuration.Inf
      )

      Await.result(
        db.readJson(
          s"SELECT time,type,secondTag,stream,severity,detection_latency,description FROM ${SeedData.event.withTags.eventType}")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = SeedData.event.withTags.eventType,
              stream = arr.get(3).toString.drop(1).dropRight(1),
              severity = arr.get(4).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(5).asInt),
              description = arr.get(6).asString,
              tags = Map(
                "type" -> arr.get(1).asString,
                "secondTag" -> arr.get(2).asString
              )
            ) shouldBe SeedData.event.withTags
          }),
        ScalaDuration.Inf
      )
    }
  }
}
