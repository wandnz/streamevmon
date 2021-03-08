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

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.SerializableInetAddress._
import nz.net.wand.streamevmon.test.TestBase

import java.net.InetAddress

class HostFunctionTest extends TestBase {
  "Hosts" should {
    "equal themselves" in {
      val a = Host(
        Set("wand.net.nz"),
        Set(),
        Set(),
        None
      )
      a shouldBe a
      a.hashCode shouldBe a.hashCode
      a.uid shouldBe a.uid
    }

    "not equal other hosts" in {
      val a = Host(
        Set("wand.net.nz"),
        Set(),
        Set(),
        None
      )
      val b = Host(
        Set("google.com"),
        Set(),
        Set(),
        None
      )
      a should not be b
      // The following isn't actually strictly true - hashCodes are allowed to
      // equal even if equals() does not.
      a.hashCode should not be b.hashCode
      a.uid should not be b.uid
    }

    "not equal similar hosts" in {
      val a = Host(
        Set("wand.net.nz"),
        Set(),
        Set(),
        None
      )
      val b = Host(
        Set("wand.net.nz", "google.com"),
        Set(),
        Set(),
        None
      )
      a should not be b
      // The following isn't actually strictly true - hashCodes are allowed to
      // equal even if equals() does not.
      a.hashCode should not be b.hashCode
      a.uid should not be b.uid
    }
  }

  "AS filter functions" should {
    "work" when {
      val singleAddress = Host(
        Set(),
        Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
        Set(),
        None
      )
      val singleHostnameItdk = Host(
        Set("wand.net.nz"),
        Set(),
        Set(),
        Some((1, AsNumber(1)))
      )
      val multipleAddresses = Host(
        Set(),
        Set(
          (InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress),
          (InetAddress.getByName("8.8.8.8"), AsNumber(1))
        ),
        Set(),
        None
      )
      val multipleAddressesItdk = Host(
        Set(),
        Set(
          (InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress),
          (InetAddress.getByName("8.8.8.8"), AsNumber(1))
        ),
        Set(),
        Some((1, AsNumber(2)))
      )

      "allAsNumbers" in {
        singleAddress.allAsNumbers shouldBe Set(AsNumber.PrivateAddress)
        singleHostnameItdk.allAsNumbers shouldBe Set(AsNumber(1))
        multipleAddresses.allAsNumbers shouldBe Set(
          AsNumber.PrivateAddress,
          AsNumber(1)
        )
        multipleAddressesItdk.allAsNumbers shouldBe Set(
          AsNumber.PrivateAddress,
          AsNumber(1),
          AsNumber(2)
        )
      }

      "validAsNumbers" in {
        singleAddress.validAsNumbers shouldBe Set()
        singleHostnameItdk.validAsNumbers shouldBe Set(AsNumber(1))
        multipleAddresses.validAsNumbers shouldBe Set(AsNumber(1))
        multipleAddressesItdk.validAsNumbers shouldBe Set(
          AsNumber(1),
          AsNumber(2)
        )
      }

      "primaryAsNumber" in {
        singleAddress.primaryAsNumber shouldBe None
        singleHostnameItdk.primaryAsNumber shouldBe Some(AsNumber(1))
        multipleAddresses.primaryAsNumber shouldBe Some(AsNumber(1))
        multipleAddressesItdk.primaryAsNumber shouldBe Some(AsNumber(2))
      }
    }
  }

  "equal and sharesWith functions" should {
    "work" when {
      val none = Host(
        Set(),
        Set(),
        Set((1, 1, 1)),
        None
      )
      val wand = Host(
        Set("wand.net.nz"),
        Set((InetAddress.getByName("130.217.250.15"), AsNumber(681))),
        Set(),
        None
      )
      val google = Host(
        Set("google.com"),
        Set((InetAddress.getByName("142.250.71.78"), AsNumber(15169))),
        Set(),
        None
      )
      val wandAndGoogle = Host(
        Set("wand.net.nz", "google.com"),
        Set((InetAddress.getByName("130.217.250.15"), AsNumber(681)), (InetAddress.getByName("142.250.71.78"), AsNumber(15169))),
        Set(),
        None
      )
      val noUids = wand
      val oneUid = none
      val multipleUids = Host(
        Set(),
        Set(),
        Set((1, 1, 1), (1, 1, 2)),
        None
      )
      val differentUid = Host(
        Set(),
        Set(),
        Set((1, 1, 2)),
        None
      )

      // We test every possible combination, since we want to make sure that
      // all the cases work in reverse as well.
      "hostnames" in {
        none.equalHostnames(none) shouldBe true
        none.sharesHostnamesWith(none) shouldBe false
        none.equalHostnames(wand) shouldBe false
        none.sharesHostnamesWith(wand) shouldBe false
        none.equalHostnames(wandAndGoogle) shouldBe false
        none.sharesHostnamesWith(wandAndGoogle) shouldBe false

        wand.equalHostnames(none) shouldBe false
        wand.sharesHostnamesWith(none) shouldBe false
        wand.equalHostnames(wand) shouldBe true
        wand.sharesHostnamesWith(wand) shouldBe true
        wand.equalHostnames(google) shouldBe false
        wand.sharesHostnamesWith(google) shouldBe false
        wand.equalHostnames(wandAndGoogle) shouldBe false
        wand.sharesHostnamesWith(wandAndGoogle) shouldBe true

        google.equalHostnames(none) shouldBe false
        google.sharesHostnamesWith(none) shouldBe false
        google.equalHostnames(wand) shouldBe false
        google.sharesHostnamesWith(wand) shouldBe false
        google.equalHostnames(google) shouldBe true
        google.sharesHostnamesWith(google) shouldBe true
        google.equalHostnames(wandAndGoogle) shouldBe false
        google.sharesHostnamesWith(wandAndGoogle) shouldBe true

        wandAndGoogle.equalHostnames(none) shouldBe false
        wandAndGoogle.sharesHostnamesWith(none) shouldBe false
        wandAndGoogle.equalHostnames(wand) shouldBe false
        wandAndGoogle.sharesHostnamesWith(wand) shouldBe true
        wandAndGoogle.equalHostnames(google) shouldBe false
        wandAndGoogle.sharesHostnamesWith(google) shouldBe true
        wandAndGoogle.equalHostnames(wandAndGoogle) shouldBe true
        wandAndGoogle.sharesHostnamesWith(wandAndGoogle) shouldBe true
      }

      "addresses" in {
        none.equalAddresses(none) shouldBe true
        none.sharesAddressesWith(none) shouldBe false
        none.equalAddresses(wand) shouldBe false
        none.sharesAddressesWith(wand) shouldBe false
        none.equalAddresses(wandAndGoogle) shouldBe false
        none.sharesAddressesWith(wandAndGoogle) shouldBe false

        wand.equalAddresses(none) shouldBe false
        wand.sharesAddressesWith(none) shouldBe false
        wand.equalAddresses(wand) shouldBe true
        wand.sharesAddressesWith(wand) shouldBe true
        wand.equalAddresses(google) shouldBe false
        wand.sharesAddressesWith(google) shouldBe false
        wand.equalAddresses(wandAndGoogle) shouldBe false
        wand.sharesAddressesWith(wandAndGoogle) shouldBe true

        google.equalAddresses(none) shouldBe false
        google.sharesAddressesWith(none) shouldBe false
        google.equalAddresses(wand) shouldBe false
        google.sharesAddressesWith(wand) shouldBe false
        google.equalAddresses(google) shouldBe true
        google.sharesAddressesWith(google) shouldBe true
        google.equalAddresses(wandAndGoogle) shouldBe false
        google.sharesAddressesWith(wandAndGoogle) shouldBe true

        wandAndGoogle.equalAddresses(none) shouldBe false
        wandAndGoogle.sharesAddressesWith(none) shouldBe false
        wandAndGoogle.equalAddresses(wand) shouldBe false
        wandAndGoogle.sharesAddressesWith(wand) shouldBe true
        wandAndGoogle.equalAddresses(google) shouldBe false
        wandAndGoogle.sharesAddressesWith(google) shouldBe true
        wandAndGoogle.equalAddresses(wandAndGoogle) shouldBe true
        wandAndGoogle.sharesAddressesWith(wandAndGoogle) shouldBe true
      }

      "ampTracerouteUids" in {
        noUids.equalAmpTracerouteUids(noUids) shouldBe true
        noUids.sharesAmpTracerouteUidsWith(noUids) shouldBe false
        noUids.equalAmpTracerouteUids(oneUid) shouldBe false
        noUids.sharesAmpTracerouteUidsWith(oneUid) shouldBe false
        noUids.equalAmpTracerouteUids(multipleUids) shouldBe false
        noUids.sharesAmpTracerouteUidsWith(multipleUids) shouldBe false
        noUids.equalAmpTracerouteUids(differentUid) shouldBe false
        noUids.sharesAmpTracerouteUidsWith(differentUid) shouldBe false

        oneUid.equalAmpTracerouteUids(noUids) shouldBe false
        oneUid.sharesAmpTracerouteUidsWith(noUids) shouldBe false
        oneUid.equalAmpTracerouteUids(oneUid) shouldBe true
        oneUid.sharesAmpTracerouteUidsWith(oneUid) shouldBe true
        oneUid.equalAmpTracerouteUids(multipleUids) shouldBe false
        oneUid.sharesAmpTracerouteUidsWith(multipleUids) shouldBe true
        oneUid.equalAmpTracerouteUids(differentUid) shouldBe false
        oneUid.sharesAmpTracerouteUidsWith(differentUid) shouldBe false

        multipleUids.equalAmpTracerouteUids(noUids) shouldBe false
        multipleUids.sharesAmpTracerouteUidsWith(noUids) shouldBe false
        multipleUids.equalAmpTracerouteUids(oneUid) shouldBe false
        multipleUids.sharesAmpTracerouteUidsWith(oneUid) shouldBe true
        multipleUids.equalAmpTracerouteUids(multipleUids) shouldBe true
        multipleUids.sharesAmpTracerouteUidsWith(multipleUids) shouldBe true
        multipleUids.equalAmpTracerouteUids(differentUid) shouldBe false
        multipleUids.sharesAmpTracerouteUidsWith(differentUid) shouldBe true

        differentUid.equalAmpTracerouteUids(noUids) shouldBe false
        differentUid.sharesAmpTracerouteUidsWith(noUids) shouldBe false
        differentUid.equalAmpTracerouteUids(oneUid) shouldBe false
        differentUid.sharesAmpTracerouteUidsWith(oneUid) shouldBe false
        differentUid.equalAmpTracerouteUids(multipleUids) shouldBe false
        differentUid.sharesAmpTracerouteUidsWith(multipleUids) shouldBe true
        differentUid.equalAmpTracerouteUids(differentUid) shouldBe true
        differentUid.sharesAmpTracerouteUidsWith(differentUid) shouldBe true
      }
    }
  }

  "mergeWith" should {
    "fail" when {
      "given mismatched ITDK node IDs" in {
        an[IllegalArgumentException] shouldBe thrownBy {
          Host(
            Set("wand.net.nz"),
            Set(),
            Set(),
            Some((1, AsNumber(1)))
          ).mergeWith(Host(
            Set("google.com"),
            Set(),
            Set(),
            Some((2, AsNumber(2)))
          ))
        }
      }

      "given mismatched anonymous hosts" in {
        an[IllegalArgumentException] shouldBe thrownBy {
          Host(
            Set(),
            Set(),
            Set((1, 1, 1)),
            None
          ).mergeWith(Host(
            Set(),
            Set(),
            Set((2, 2, 2)),
            None
          ))
        }
      }
    }

    "work" when {
      "hosts are identical" in {
        {
          val a = Host(
            Set("wand.net.nz"),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            Some((1, AsNumber(1)))
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set(),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            Some((1, AsNumber(1)))
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set("wand.net.nz"),
            Set(),
            Set(),
            Some((1, AsNumber(1)))
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set("wand.net.nz"),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            None
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set(),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            None
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set("wand.net.nz"),
            Set(),
            Set(),
            None
          )
          a.mergeWith(a) shouldBe a
        }
        {
          val a = Host(
            Set(),
            Set(),
            Set((1, 1, 1)),
            None
          )
          a.mergeWith(a) shouldBe a
        }
      }

      "hostnames are different" in {
        Host(
          Set("wand.net.nz"),
          Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
          Set(),
          Some((1, AsNumber(1)))
        ).mergeWith(
          Host(
            Set("google.com"),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            Some((1, AsNumber(1)))
          )
        )

        Host(
          Set("wand.net.nz"),
          Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
          Set(),
          None
        ).mergeWith(
          Host(
            Set("google.com"),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            None
          )
        )
      }

      "addresses are different" in {
        Host(
          Set("wand.net.nz"),
          Set((InetAddress.getByName("130.217.250.15"), AsNumber(681))),
          Set(),
          Some((1, AsNumber(1)))
        ).mergeWith(
          Host(
            Set("wand.net.nz"),
            Set((InetAddress.getByName("142.250.71.78"), AsNumber(15169))),
            Set(),
            Some((1, AsNumber(1)))
          )
        )

        Host(
          Set("wand.net.nz"),
          Set((InetAddress.getByName("130.217.250.15"), AsNumber(681))),
          Set(),
          None
        ).mergeWith(
          Host(
            Set("wand.net.nz"),
            Set((InetAddress.getByName("142.250.71.78"), AsNumber(15169))),
            Set(),
            None
          )
        )
      }
    }
  }

  "mergeAnonymous" should {
    "fail" when {
      "given one or more non-anonymous hosts" in {
        an[IllegalArgumentException] shouldBe thrownBy {
          val a = Host(
            Set("wand.net.nz"),
            Set(),
            Set(),
            None
          )
          a.mergeAnonymous(a)
        }
        an[IllegalArgumentException] shouldBe thrownBy {
          val a = Host(
            Set(),
            Set((InetAddress.getByName("127.0.0.1"), AsNumber.PrivateAddress)),
            Set(),
            None
          )
          a.mergeAnonymous(a)
        }
      }
    }

    "work" in {
      val a = Host(
        Set(),
        Set(),
        Set((1, 1, 1)),
        None
      )
      val b = Host(
        Set(),
        Set(),
        Set((2, 2, 2)),
        None
      )
      val c = Host(
        Set(),
        Set(),
        Set((1, 1, 1), (2, 2, 2)),
        None
      )

      a.mergeAnonymous(a) shouldBe a
      b.mergeAnonymous(b) shouldBe b
      c.mergeAnonymous(c) shouldBe c
      a.mergeAnonymous(b) shouldBe c
      b.mergeAnonymous(a) shouldBe c
      c.mergeAnonymous(a) shouldBe c
      c.mergeAnonymous(b) shouldBe c
    }
  }
}
