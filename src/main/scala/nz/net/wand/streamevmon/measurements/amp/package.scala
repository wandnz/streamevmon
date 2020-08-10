package nz.net.wand.streamevmon.measurements

/** This package represents data gathered by [[https://amp.wand.net.nz/ AMP]],
  * WAND's Active Measurement Project. It should be a comprehensive
  * representation of the measurements as they are stored in InfluxDB.
  *
  * As such, all AMP measurements are [[InfluxMeasurement InfluxMeasurements]].
  * AMP measurements can be enriched into [[RichInfluxMeasurement RichInfluxMeasurements]]
  * using [[InfluxMeasurementFactory.enrichMeasurement]].
  *
  * The metadata used to enrich a measurement is obtained from PostgreSQL (see
  * [[nz.net.wand.streamevmon.connectors.postgres Postgres connector package]]),
  * and can be obtained separately as a
  * [[nz.net.wand.streamevmon.measurements.amp.PostgresMeasurementMeta MeasurementMeta]].
  * The metadata is information about the test schedule which produced the
  * measurement.
  *
  */
package object amp {}
