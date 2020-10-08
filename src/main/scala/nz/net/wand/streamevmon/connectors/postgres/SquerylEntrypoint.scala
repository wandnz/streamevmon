package nz.net.wand.streamevmon.connectors.postgres

import org.squeryl.customtypes.CustomTypesMode

/** Importing this allows use of PostgreSQL queries. Should be used in
  * conjunction with [[PostgresSchema]].
  */
object SquerylEntrypoint extends CustomTypesMode {}
