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

package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction

import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}

/** This enum includes logic to build sinks. */
object SinkType extends Enumeration {

  val Influx: ValueBuilder = new ValueBuilder("influx")
  val Print: ValueBuilder = new ValueBuilder("print")

  class ValueBuilder(name: String) extends Val(name) {
    def build: SinkFunction[Event] with HasFlinkConfig = {
      this match {
        case Influx => new InfluxSinkFunction
        case Print => new PrintSinkFunction[Event] with HasFlinkConfig {
          override val flinkName: String = "Print: Std Out"
          override val flinkUid: String = "print-sink"
          override val configKeyGroup: String = ValueBuilder.this.name

          // Override configurations can be supplied to the print sink, but will be ignored.
        }
        case t => throw new UnsupportedOperationException(s"Unknown sink type $t")
      }
    }
  }
}
