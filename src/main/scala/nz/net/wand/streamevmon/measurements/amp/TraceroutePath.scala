package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.connectors.postgres.InetPath

case class TraceroutePath(
  path_id: Int,
  length: Int,
  path: InetPath
) {

}
