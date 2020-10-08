package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.connectors.postgres.InetPath

/** An [[nz.net.wand.streamevmon.connectors.postgres.InetPath InetPath]], with
  * some extra metadata. This represents the table named
  * `data_amp_traceroute_paths_{aspath_id}` in PostgreSQL.
  *
  * The `length` field here should be the same as the length of `path`.
  */
case class TraceroutePath(
  path_id: Int,
  length: Int,
  path: InetPath
)
