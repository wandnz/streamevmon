package nz.net.wand.streamevmon.connectors

import org.squeryl.PrimitiveTypeMode

/** Importing this allows use of PostgreSQL queries. Should be used in
  * conjunction with [[PostgresSchema]].
  *
  * Used in [[PostgresConnection]].
  */
object SquerylEntrypoint extends PrimitiveTypeMode {}
