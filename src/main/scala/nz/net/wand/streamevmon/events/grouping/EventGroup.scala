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

import com.github.fsanaulla.chronicler.core.alias.ErrorOr
import com.github.fsanaulla.chronicler.core.model.InfluxWriter

/** A group of events. If `endTime` is none, then the event is still ongoing. */
case class EventGroup(
  startTime: Instant,
  endTime  : Option[Instant],
  events   : Iterable[Event]
) {
  lazy val modeEventType: String = events.groupBy(_.eventType).maxBy(_._2.size)._1
  lazy val streams: Iterable[String] = events.map(_.stream)
  lazy val meanSeverity: Int = events.map(_.severity).sum / events.size
  lazy val meanDetectionLatency: Duration = Duration.ofNanos(events.map(_.detectionLatency.toNanos).sum / events.size)

  def toLineProtocol: String = {

    val tags = Seq(
      endTime.map { t =>
        s"endTime=${TimeUnit.MILLISECONDS.toNanos(t.toEpochMilli)}i"
      }.getOrElse(""),
      s"""streams=\"[${streams.mkString(";")}]\""""
    ).filterNot(_ == "")

    val fields = Seq(
      s"""modeEventType=\"$modeEventType\"""",
      s"meanSeverity=${meanSeverity}i",
      s"meanDetectionLatency=${meanDetectionLatency.toNanos}i"
    )

    val time = s"${TimeUnit.MILLISECONDS.toNanos(startTime.toEpochMilli)}"

    val r = s"${tags.mkString(",")} ${fields.mkString(",")} $time"
    r
  }
}

object EventGroup {
  def getMeasurementName(value: EventGroup): String = "event_group"

  def getWriter[T <: EventGroup]: InfluxWriter[T] = EventGroupWriter[T]()

  case class EventGroupWriter[T <: EventGroup]() extends InfluxWriter[T] {
    override def write(obj: T): ErrorOr[String] = Right(obj.toLineProtocol)
  }
}
