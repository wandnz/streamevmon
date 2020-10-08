package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.connectors.postgres.AsPath

case class TracerouteAsPath(
  aspath_id: Int,
  aspath_length: Int,
  uniqueas: Int,
  responses: Int,
  aspath: AsPath
) {

}
