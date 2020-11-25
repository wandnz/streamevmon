package nz.net.wand.streamevmon.measurements

/** This package represents data from the ESnet perfSONAR esmond API.
  *
  * [[nz.net.wand.streamevmon.measurements.esmond.EsmondMeasurement EsmondMeasurement]]
  * is the top-level class, from which all other classes inherit. There is also
  * the [[nz.net.wand.streamevmon.measurements.esmond.RichEsmondMeasurement RichEsmondMeasurement]],
  * which classes that contain a bit of extra metadata inherit from.
  *
  * Each type of EsmondMeasurement represents one or more `eventType`s, depending
  * on the format of their `value` field. The `stream` field is derived directly
  * from `metadataKey`, which is unique for a particular data stream.
  *
  * Some EsmondMeasurements implement
  * [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]] and
  * [[nz.net.wand.streamevmon.measurements.traits.CsvOutputable CsvOutputable]],
  * but others have more complex value types and do not.
  *
  * To obtain EsmondMeasurements, you should use the
  * [[nz.net.wand.streamevmon.flink.sources.PollingEsmondSourceFunction PollingEsmondSourceFunction]],
  * which produces RichEsmondMeasurements.
  */
package object esmond {}
