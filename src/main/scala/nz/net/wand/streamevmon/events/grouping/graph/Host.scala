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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.connectors.postgres.schema.{AsNumber, AsNumberCategory}

case class Host(
  hostnames: Set[String],
  addresses       : Set[(SerializableInetAddress, AsNumber)],
  ampTracerouteUid: Option[(Int, Int, Int)],
  itdkNodeId: Option[(Int, AsNumber)]
) extends Serializable {

  if (ampTracerouteUid.isDefined) {
    if (hostnames.nonEmpty || addresses.nonEmpty) {
      throw new IllegalArgumentException("Traceroute UID is defined, but a hostname or address is known!")
    }
  }
  else {
    if (hostnames.isEmpty && addresses.isEmpty) {
      throw new IllegalArgumentException("Traceroute UID is undefined, but no hostnames or addresses are known!")
    }
  }

  if (itdkNodeId.isDefined) {
    if (hostnames.isEmpty && addresses.isEmpty) {
      throw new IllegalArgumentException("ITDK node ID is defined, but no hostnames or addresses are known!")
    }
  }

  lazy val allAsNumbers: Set[AsNumber] = {
    addresses.map(_._2) ++ itdkNodeId.map(_._2)
  }

  val validAsNumbers: Set[AsNumber] = {
    allAsNumbers.filter(_.category == AsNumberCategory.Valid)
  }

  val uid: String = {
    itdkNodeId match {
      case Some(value) => s"N$value"
      case None => if (hostnames.nonEmpty) {
        hostnames.mkString(";")
      }
      else if (addresses.nonEmpty) {
        addresses.mkString(";")
      }
      else {
        ampTracerouteUid match {
          case Some(value) => value.toString
          case None => throw new IllegalStateException("Trying to get UID for host with no data!")
        }
      }
    }
  }

  override def toString: String = {
    s"${
      if (hostnames.nonEmpty) {
        s"${hostnames.head}${
          if (hostnames.size > 1) {
            s" (and ${hostnames.size - 1} more)"
          }
          else {
            ""
          }
        } (${addresses.size} addresses)"
      }
      else if (addresses.nonEmpty) {
        s"${addresses.head._1}${
          if (addresses.size > 1) {
            s" (and ${addresses.size - 1} more)"
          }
          else {
            ""
          }
        } (${addresses.head._2})"
      }
      else {
        ampTracerouteUid.map(u => s"? $u").getOrElse("Unknown Host")
      }
    }${itdkNodeId.map(id => s" (ITDK N$id)").getOrElse("")}"
  }

  def equalHostnames(other: Host): Boolean = this.hostnames == other.hostnames

  def sharesHostnamesWith(other: Host): Boolean = {
    this.hostnames.exists(name => other.hostnames.contains(name))
  }

  def equalAddresses(other: Host): Boolean = this.addresses == other.addresses

  def sharesAddressesWith(other: Host): Boolean = {
    this.addresses.exists(addr => other.addresses.contains(addr))
  }

  def equalItdkNode(other: Host): Boolean = {
    itdkNodeId.isDefined && this.itdkNodeId == other.itdkNodeId
  }

  def mergeWith(other: Host): Host = {
    val newItdkNodeId = if (
      this.itdkNodeId.isDefined &&
        other.itdkNodeId.isDefined &&
        this.itdkNodeId != other.itdkNodeId
    ) {
      throw new IllegalArgumentException(s"ITDK Node IDs do not match! ${this.itdkNodeId.get} != ${other.itdkNodeId.get}")
    }
    else {
      Seq(this.itdkNodeId, other.itdkNodeId).flatten.headOption
    }

    val newHostnames = this.hostnames ++ other.hostnames
    val newAddresses = this.addresses ++ other.addresses

    val newTracerouteUid = if (
      newHostnames.isEmpty &&
        newAddresses.isEmpty &&
        newItdkNodeId.isEmpty
    ) {
      val uids = Set(this.ampTracerouteUid, other.ampTracerouteUid).flatten
      if (uids.size != 1) {
        throw new IllegalArgumentException("Trying to merge two anonymous hosts with different traceroute UIDs!")
      }
      else {
        this.ampTracerouteUid
      }
    }
    else {
      None
    }

    Host(
      newHostnames,
      newAddresses,
      newTracerouteUid,
      newItdkNodeId
    )
  }
}
