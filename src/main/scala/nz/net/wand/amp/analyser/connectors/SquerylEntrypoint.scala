package nz.net.wand.amp.analyser.connectors

import org.squeryl.PrimitiveTypeMode

/** Importing this allows use of PostgreSQL queries. Should be used in
  * conjunction with [[SquerylEntrypoint]].
  *
  * Used in [[PostgresConnection]].
  */
object SquerylEntrypoint extends PrimitiveTypeMode {}
