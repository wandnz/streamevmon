package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber
import nz.net.wand.streamevmon.events.grouping.graph.SerializableInetAddress._

import java.net.InetAddress

class HostConstructionTest extends TestBase {
  "Hosts" should {
    "be invalid" when {
      "ampTracerouteUids is non-empty" when {
        "one of hostnames or addresses is non-empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set((1, 1, 1)),
              None
            )
          }

          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set((1, 1, 1)),
              None
            )
          }
        }
      }

      // this is the "when all elements are empty" case
      "ampTracerouteUids is empty" when {
        "hostnames and addresses are both empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set(),
              None
            )
          }
        }
      }

      "itdkNodeId is non-empty" when {
        "hostnames and addresses are both empty" in {
          an[IllegalArgumentException] shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
        }
      }
    }

    "be valid" when {
      "ampTracerouteUids is non-empty" when {
        "hostnames and addresses are both empty" in {
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set(),
              Set((1, 1, 1)),
              None
            )
          }
        }
      }

      "itdkNodeId is empty" when {
        "at least one of hostnames or addresses is non-empty" in {
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set(),
              None
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              None
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              None
            )
          }
        }
      }

      "itdkNodeId is non-empty" when {
        "at least one of hostnames or addresses is non-empty" in {
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set(),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set(),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
          noException shouldBe thrownBy {
            Host(
              Set("wand.net.nz"),
              Set((InetAddress.getByName("192.168.0.1"), AsNumber.PrivateAddress)),
              Set(),
              Some((1, AsNumber(1)))
            )
          }
        }
      }
    }
  }
}
