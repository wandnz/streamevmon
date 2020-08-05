package nz.net.wand.streamevmon

/** Contains classes representing network measurements, such as those gathered
  * by AMP or perfSONAR.
  *
  * The base type is [[nz.net.wand.streamevmon.measurements.Measurement Measurement]].
  * This includes a timestamp and a stream ID. Concrete classes will also include
  * one or more value fields. Measurement also provides an `isLossy` function,
  * since that knowledge is useful in a wide variety of situations and can be
  * implemented in the concrete class.
  *
  * A [[nz.net.wand.streamevmon.measurements.RichMeasurement RichMeasurement]]
  * also includes some metadata about the stream it came from, such as the
  * source and destination of an ICMP test. Some Measurements can be enriched
  * into RichMeasurements when provided with some additional data.
  *
  * Any Measurement can have the additional traits [[HasDefault]] and
  * [[CsvOutputable]] mixed in, which provide additional functionality. Many
  * detectors require Measurements with HasDefault so they don't need to choose
  * which field they use.
  *
  * InfluxMeasurement and RichInfluxMeasurement are special subtypes of
  * Measurement which include both HasDefault and CsvOutputable, as well as
  * enabling additional functionality, such as InfluxDB Line Protocol support.
  *
  * See the child package objects for more specific details about the various
  * supported measurement types.
  */
package object measurements {}
