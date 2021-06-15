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
import nz.net.wand.streamevmon.flink.sinks.InfluxEventSink
import nz.net.wand.streamevmon.test.{InfluxContainerSpec, SeedData}

import java.time.{Instant, Duration => JavaDuration}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => ScalaDuration}

class InfluxEventSinkTest extends InfluxContainerSpec {
  "InfluxEventSink" should {
    "write data to InfluxDB" in {
      val sink = setupSinkFunction(new InfluxEventSink, "no-subscription")
      sink.open(null)

      sink.invoke(SeedData.event.withTags, new MockSinkContext(SeedData.event.withTags.time))
      sink.invoke(SeedData.event.withoutTags, new MockSinkContext(SeedData.event.withoutTags.time))
      sink.invoke(SeedData.event.amp2Event, new MockSinkContext(SeedData.event.amp2Event.time))

      sink.close()

      val db = InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
        .database(container.database)

      Await.result(
        db.readJson(
          s"SELECT time,event_type,stream,severity,detection_latency,description FROM events WHERE event_type='${SeedData.event.withoutTags.eventType}'")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = arr.get(1).toString.drop(1).dropRight(1),
              stream = arr.get(2).toString.drop(1).dropRight(1),
              severity = arr.get(3).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(4).asInt),
              description = arr.get(5).asString,
              tags = Map()
            ) shouldBe SeedData.event.withoutTags
          }),
        ScalaDuration.Inf
      )

      Await.result(
        db.readJson(
          s"SELECT time,event_type,type,secondTag,stream,severity,detection_latency,description FROM events WHERE event_type='${SeedData.event.withTags.eventType}'")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = arr.get(1).toString.drop(1).dropRight(1),
              stream = arr.get(4).toString.drop(1).dropRight(1),
              severity = arr.get(5).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(6).asInt),
              description = arr.get(7).asString,
              tags = Map(
                "type" -> arr.get(2).asString,
                "secondTag" -> arr.get(3).asString
              )
            ) shouldBe SeedData.event.withTags
          }),
        ScalaDuration.Inf
      )

      Await.result(
        db.readJson(
          s"SELECT time,event_type,stream,severity,detection_latency,description,source,destination,test FROM events WHERE event_type='${SeedData.event.amp2Event.eventType}'")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            val result = Event(
              eventType = arr.get(1).toString.drop(1).dropRight(1),
              stream = arr.get(2).toString.drop(1).dropRight(1),
              severity = arr.get(3).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(4).asInt),
              description = arr.get(5).asString,
              tags = Map()
            )

            result shouldBe SeedData.event.amp2Event
          }),
        ScalaDuration.Inf
      )
    }
  }
}
