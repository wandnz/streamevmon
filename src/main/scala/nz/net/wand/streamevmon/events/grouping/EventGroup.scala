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

package nz.net.wand.streamevmon.events.grouping

import nz.net.wand.streamevmon.events.Event

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import java.util.UUID

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxWriter

import scala.compat.Platform

/** A group of events. If `endTime` is none, then the event is still ongoing. */
case class EventGroup(
  startTime: Instant,
  endTime   : Option[Instant],
  private val memberEvents: Iterable[Event],
  uuid: Option[UUID] = None
) {
  /** Returns a new EventGroup with a fresh UUID. Applies the supplied UUID, or
    * generates a new one by default.
    */
  def withUuid(id: Option[UUID] = None): EventGroup = id match {
    case Some(_) => EventGroup(startTime, endTime, memberEvents, id)
    case None => EventGroup(startTime, endTime, memberEvents, Some(UUID.randomUUID()))
  }

  lazy val modeEventType: String = memberEvents.groupBy(_.eventType).maxBy(_._2.size)._1
  lazy val streams: Iterable[String] = memberEvents.map(_.stream).toSet
  lazy val meanSeverity: Int = memberEvents.map(_.severity).sum / memberEvents.size
  lazy val meanDetectionLatency: Duration = Duration.ofNanos(memberEvents.map(_.detectionLatency.toNanos).sum / memberEvents.size)
  lazy val description: String = {
    endTime match {
      case Some(eTime) =>
        s"""Group of ${memberEvents.size} events over ${streams.size} measurement
           |streams with duration ${Duration.between(startTime, eTime)},
           |most common event type \"$modeEventType\",
           |mean severity $meanSeverity,
           |and mean detection latency $meanDetectionLatency""".stripMargin.replace(Platform.EOL, " ")
      case None => "Event group still in progress."
    }
  }

  def toLineProtocol: String = {
    val tags = Seq(
      endTime.map { t =>
        s"endTime=${TimeUnit.MILLISECONDS.toNanos(t.toEpochMilli)}i"
      }.getOrElse(""),
      uuid.map(u => s"""uid=${u.toString}""").getOrElse(""),
    ).filterNot(_ == "")

    val fields = Seq(
      s"meanDetectionLatency=${meanDetectionLatency.toNanos}i",
      s"meanSeverity=${meanSeverity}i",
      s"""modeEventType=\"$modeEventType\"""",
      s"event_count=${memberEvents.size}i",
      s"""description=\"$description\""""
    )

    val time = s"${TimeUnit.MILLISECONDS.toNanos(startTime.toEpochMilli)}"

    val r = if (tags.isEmpty) {
      // This case shouldn't happen outside tests, but if it does, chronicler generates invalid line protocol.
      s"still_ongoing=true ${fields.mkString(",")} $time"
    }
    else {
      s"${tags.mkString(",")} ${fields.mkString(",")} $time"
    }
    r
  }

  /** The underlying group of events is private. We instead expose a new group
    * where each event has an additional tag `in_group` which declares group
    * membership. This is mainly used to write membership-rich events to InfluxDB.
    *
    * If no UUID is defined, this just passes the events with no group.
    */
  def events: Iterable[Event] = uuid match {
    case Some(uid) => memberEvents.map { e =>
      Event(
        e.eventType,
        e.stream,
        e.severity,
        e.time,
        e.detectionLatency,
        e.description,
        e.tags + ("in_group" -> uid.toString)
      )
    }
    case None => memberEvents
  }
}

object EventGroup {
  def getMeasurementName(value: EventGroup): String = "event_groups"

  def getWriter[T <: EventGroup]: InfluxWriter[T] = EventGroupWriter[T]()

  case class EventGroupWriter[T <: EventGroup]() extends InfluxWriter[T] {
    override def write(obj: T): ErrorOr[String] = Right(obj.toLineProtocol)
  }
}
