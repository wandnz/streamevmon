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

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber

import java.net.InetAddress

class HostEqualityTest extends TestBase {
  def checkEqualAndHashCode(a: => Any, b: => Any): Unit = {
    a shouldEqual b
    a.hashCode shouldEqual b.hashCode
  }

  "Host with known hostname" should {
    "equal hosts with matching hostnames" in {
      def noAddresses = new HostWithKnownHostname("example.com", Set())

      def oneAddress = new HostWithKnownHostname("example.com", Set((InetAddress.getByName("0.0.0.0"), AsNumber(1))))

      def differentAddress = new HostWithKnownHostname("example.com", Set((InetAddress.getByName("8.8.8.8"), AsNumber(1))))

      def differentAs = new HostWithKnownHostname("example.com", Set((InetAddress.getByName("0.0.0.0"), AsNumber(2))))

      def differentBoth = new HostWithKnownHostname("example.com", Set((InetAddress.getByName("8.8.8.8"), AsNumber(2))))

      checkEqualAndHashCode(noAddresses, noAddresses)
      checkEqualAndHashCode(oneAddress, oneAddress)
      checkEqualAndHashCode(oneAddress, differentAddress)
      checkEqualAndHashCode(oneAddress, differentAs)
      checkEqualAndHashCode(oneAddress, differentBoth)
    }

    "not equal hosts that only share addresses" in {
      new HostWithKnownHostname(
        "example.com",
        Set((InetAddress.getByName("0.0.0.0"), AsNumber(1)))
      ) should not equal new HostWithKnownHostname(
        "example.co.nz",
        Set((InetAddress.getByName("0.0.0.0"), AsNumber(1)))
      )
    }

    "not equal hosts that share neither hostnames or addresses" in {
      def com = new HostWithKnownHostname("example.com", Set((InetAddress.getByName("0.0.0.0"), AsNumber(1))))

      def differentAddress = new HostWithKnownHostname("example.co.nz", Set((InetAddress.getByName("8.8.8.8"), AsNumber(1))))

      def differentAs = new HostWithKnownHostname("example.co.nz", Set((InetAddress.getByName("0.0.0.0"), AsNumber(2))))

      def allDifferent = new HostWithKnownHostname("example.co.nz", Set((InetAddress.getByName("8.8.8.8"), AsNumber(2))))

      com should not equal differentAddress
      com should not equal differentAs
      com should not equal allDifferent
    }

    "not equal hosts with different knowledge levels" in {
      new HostWithKnownHostname(
        "example.com",
        Set((InetAddress.getByName("0.0.0.0"), AsNumber(1)))
      ) should not equal new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      )

      new HostWithKnownHostname(
        "example.com",
        Set((InetAddress.getByName("0.0.0.0"), AsNumber(1)))
      ) should not equal new HostWithUnknownAddress(0, 0, 0)
    }
  }

  "Host with unknown hostname" should {
    "equal hosts with matching addresses" in {
      def a = new HostWithUnknownHostname((InetAddress.getByName("0.0.0.0"), AsNumber(1)))

      checkEqualAndHashCode(a, a)
    }

    "not equal hosts with mismatched addresses" in {
      new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      ) should not equal new HostWithUnknownHostname(
        (InetAddress.getByName("8.8.8.8"), AsNumber(1))
      )

      new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      ) should not equal new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(2))
      )

      new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      ) should not equal new HostWithUnknownHostname(
        (InetAddress.getByName("8.8.8.8"), AsNumber(2))
      )
    }

    "not equal hosts with different knowledge levels" in {
      new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      ) should not equal new HostWithKnownHostname(
        "example.com",
        Set((InetAddress.getByName("0.0.0.0"), AsNumber(1)))
      )

      new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      ) should not equal new HostWithUnknownAddress(0, 0, 0)
    }
  }

  "Host with unknown hostname and address" should {
    "equal hosts with the same UID" in {
      checkEqualAndHashCode(new HostWithUnknownAddress(0, 0, 0), new HostWithUnknownAddress(0, 0, 0))
    }

    "not equal hosts with different UIDs" in {
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(0, 0, 1)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(0, 1, 0)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(1, 0, 0)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(0, 1, 1)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(1, 0, 1)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(1, 1, 0)
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownAddress(1, 1, 1)
    }

    "not equal hosts with different knowledge levels" in {
      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithKnownHostname(
        "example.com",
        Set()
      )

      new HostWithUnknownAddress(0, 0, 0) should not equal new HostWithUnknownHostname(
        (InetAddress.getByName("0.0.0.0"), AsNumber(1))
      )
    }
  }
}
