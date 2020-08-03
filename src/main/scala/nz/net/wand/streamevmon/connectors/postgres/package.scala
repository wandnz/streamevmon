package nz.net.wand.streamevmon.connectors

/** This package contains an interface to PostgreSQL that produces
  * [[nz.net.wand.streamevmon.measurements.amp.MeasurementMeta MeasurementMeta]]
  * objects. Create a
  * [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection PostgresConnection]],
  * and call its [[nz.net.wand.streamevmon.connectors.postgres.PostgresConnection.getMeta getMeta]] function.
  *
  * ==Configuration==
  *
  * This module is configured by the `source.postgres` config key group.
  *
  * - `serverName`: The hostname which the PostgreSQL database can be reached on.
  * Default: "localhost"
  *
  * - `portNumber`: The port that PostgreSQL is running on.
  * Default: 5432
  *
  * - `databaseName`: The database which the metadata is stored in.
  * Default: "nntsc"
  *
  * - `user`: The username which should be used to connect to the database.
  * Default: "cuz"
  *
  * - `password`: The password which should be used to connect to the database.
  * Default: ""
  *
  * This module uses [[nz.net.wand.streamevmon.Caching Caching]]. The TTL value
  * of the cached results can be set with the `caching.ttl` configuration key,
  * which defaults to 30 seconds.
  */
package object postgres {}
