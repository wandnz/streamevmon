package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.SerializableInetAddress._

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

  "mergeWith" ignore {
    "fail" when {
      "given mismatched ITDK node IDs" in {

      }

      "given mismatched anonymous hosts" in {

      }
    }

    "work" in {

    }
  }

  "mergeAnonymous" ignore {
    "fail" when {
      "given one or more non-anonymous hosts" in {

      }
    }

    "work" in {

    }
  }
}
