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

package nz.net.wand.streamevmon.connectors.postgres.schema

import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

/** Combines an AsPath and an InetPath, as though they were zipped. This ties
  * each InetAddress with its corresponding AsNumber, as reported by the data
  * source.
  */
case class AsInetPath(
  private val path: Iterable[AsInetPathEntry],
  measurement     : Traceroute,
  meta            : TracerouteMeta
) extends Iterable[AsInetPathEntry] {
  override def iterator: Iterator[AsInetPathEntry] = path.iterator
}

object AsInetPath {
  def apply(
    inetPath: InetPath,
    asPath: Option[AsPath],
    measurement        : Traceroute,
    meta: TracerouteMeta
  ): AsInetPath = {
    AsInetPath(
      asPath match {
        case Some(asPathValue) =>
          inetPath.zip(asPathValue.expandedPath).map { case (inet, asn) =>
            AsInetPathEntry(inet, asn)
          }
        case None =>
          inetPath.map { inet =>
            AsInetPathEntry(inet, AsNumber.Missing)
          }
      },
      measurement,
      meta
    )
  }
}
