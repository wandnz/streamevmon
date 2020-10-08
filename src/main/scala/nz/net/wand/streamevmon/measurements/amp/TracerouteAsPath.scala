package nz.net.wand.streamevmon.measurements.amp

import nz.net.wand.streamevmon.connectors.postgres.AsPath

/** An [[nz.net.wand.streamevmon.connectors.postgres.AsPath AsPath]], with some
  * extra metadata. This represents the table named
  * `data_amp_traceroute_aspaths_{aspath_id}` in PostgreSQL.
  *
  * `aspath_length` should be the same as the sum of the `hopsInAs` fields of
  * the elements of `aspath`.
  *
  * `uniqueas` should be the same as the length of `aspath`.
  *
  * `responses` should be the same as the sum of the `hopsInAs` fields of those
  * elements of `aspath` that are not in the `Missing`
  * [[nz.net.wand.streamevmon.connectors.postgres.AsNumberCategory AsNumberCategory]].
  */
case class TracerouteAsPath(
  aspath_id: Int,
  aspath_length: Int,
  uniqueas: Int,
  responses: Int,
  aspath: AsPath
)
