package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.TestBase

class TemporaryBigDataInfluxHistoryTest extends TestBase {

  def getInflux: InfluxHistoryConnection = {
    InfluxHistoryConnection(
      "bigdata",
      "autogen",
      "192.168.122.73",
      8086,
      "bigdata",
      "bigdata"
    )
  }

  "InfluxHistoryConnection" should {
    "read bigdata results" when {
      "type is flow_statistics" in {
        getInflux.getFlowStatistics()
      }
    }
  }
}
