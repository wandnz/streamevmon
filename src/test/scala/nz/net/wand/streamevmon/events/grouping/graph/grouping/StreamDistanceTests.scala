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

package nz.net.wand.streamevmon.events.grouping.graph.grouping

import nz.net.wand.streamevmon.events.grouping.graph.impl.GraphType.{EdgeT, GraphT, VertexT}
import nz.net.wand.streamevmon.measurements.amp.TracerouteMeta
import nz.net.wand.streamevmon.test.TestBase

import java.time.Instant
import java.util.Objects

trait StreamDistanceTests extends TestBase {
  val baseSource = "src-1"
  val nearSource = "src-2"
  val farSource = "src-3"
  val baseDest = "dst-1"
  val nearDest = "dst-2"
  val farDest = "dst-3"

  def metaOf(src: String, dst: String): TracerouteMeta = TracerouteMeta(
    Objects.hash(src, dst),
    src,
    dst,
    "ipv4",
    "84"
  )

  def hostOf(name: Option[String] = None, manualUid: Option[Int] = None): VertexT = {
    val (hosts: Set[String], uids: Set[(Int, Int, Int)]) = (name, manualUid) match {
      case (Some(n), _) => (Set(n), Set())
      case (None, Some(n)) => (Set(), Set((n, n, n)))
      case (None, None) =>
        uid += 1
        (Set(), Set((uid, uid, uid)))
    }

    new VertexT(
      hosts,
      Set(),
      uids,
      None
    )
  }

  var uid = 0

  def edge: EdgeT = {
    uid += 1
    new EdgeT(Instant.ofEpochSecond(uid), uid.toString)
  }

  def makeGraph(): GraphT = {
    val g = new GraphT(classOf[EdgeT])

    // slap some known hosts in
    Seq(baseDest, baseSource, nearDest, nearSource, farDest, farSource)
      .foreach(n => g.addVertex(hostOf(name = Some(n))))

    // add a bunch of anonymous hosts
    Range(0, 15).foreach { i =>
      g.addVertex(hostOf(manualUid = Some(i)))
    }
    // connect up three parallel lines of anonymous hosts
    Range(0, 5).sliding(2).foreach { is =>
      g.addEdge(
        hostOf(manualUid = Some(is.head)),
        hostOf(manualUid = Some(is.last)),
        edge
      )
      g.addEdge(
        hostOf(manualUid = Some(is.head + 5)),
        hostOf(manualUid = Some(is.last + 5)),
        edge
      )
      g.addEdge(
        hostOf(manualUid = Some(is.head + 10)),
        hostOf(manualUid = Some(is.last + 10)),
        edge
      )
    }

    // connect the middles of the lines of anonymous hosts
    g.addEdge(
      hostOf(manualUid = Some(2)),
      hostOf(manualUid = Some(2 + 5)),
      edge
    )
    g.addEdge(
      hostOf(manualUid = Some(2 + 5)),
      hostOf(manualUid = Some(2 + 10)),
      edge
    )

    // connect the lines to the known hosts
    Seq(
      (baseSource, 0),
      (4, baseDest),
      (nearSource, 5),
      (9, nearDest),
      (farSource, 10),
      (14, farDest)
    ).foreach { case (start, end) =>
      val (hostS, hostE) = start match {
        case s: Int => (hostOf(manualUid = Some(s)), hostOf(name = Some(end.asInstanceOf[String])))
        case s: String => (hostOf(name = Some(s)), hostOf(manualUid = Some(end.asInstanceOf[Int])))
      }
      g.addEdge(hostS, hostE, edge)
    }

    g
  }

}
