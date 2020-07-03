package nz.net.wand.streamevmon

/** Contains classes representing network measurements, such as those gathered
  * by AMP or perfSONAR.
  *
  * The base type is Measurement. Any Measurement should contain a timestamp,
  * stream ID, and some data. They can also be queried for if they represent
  * a lossy measurement. Lossy measurements lack data, but are still sent to
  * detectors.
  *
  * RichMeasurement is a subtype of Measurement, which also includes some
  * metadata about the stream which it came from, such as source and destination.
  * Some Measurements can be enriched into RichMeasurements when provided with
  * the relevant MeasurementMeta, or other equivalent type.
  *
  * Any Measurement can have the additional traits [[HasDefault]] and
  * [[CsvOutputable]] mixed in, which provide additional functionality. Many
  * detectors require Measurements with HasDefault so they don't need to choose
  * which field they use.
  *
  * InfluxMeasurement and RichInfluxMeasurement are special subtypes of
  * Measurement which include both HasDefault and CsvOutputable, as well as
  * enabling additional functionality, such as InfluxDB Line Protocol support.
  * They should include a companion object that extends InfluxMeasurementFactory.
  *
  * == Live AMP Data ==
  *
  * The [[amp]] package represents data gathered by AMP, the Active Measurement
  * Project. It should be a comprehensive representation of the measurements as
  * they are stored in InfluxDB.
  *
  * There are three types of AMP measurement object, depending on how much
  * metadata about the stream is included.
  *
  * - Classes which inherit from
  * [[InfluxMeasurement InfluxMeasurement]]
  * represent the basic measurement, complete with its stream ID. They contain
  * no metadata, but can be "enriched" using
  * [[InfluxMeasurementFactory.enrichMeasurement InfluxMeasurementFactory.enrichMeasurement]].
  * This returns an instance of a class which inherits from RichInfluxMeasurement.
  *
  * - Classes which inherit from
  * [[nz.net.wand.streamevmon.measurements.amp.MeasurementMeta MeasurementMeta]]
  * represent data gathered from PostgreSQL based on a measurement's stream ID.
  * This might include a test destination, a latency, or an address family.
  * These types also include the corresponding stream ID. They cannot be
  * directly enriched, but are used in order to enrich a Measurement. There is
  * not often a need to create MeasurementMeta objects yourself, as a
  * RichMeasurement contains all the same data.
  *
  * - Classes which inherit from
  * [[RichInfluxMeasurement RichInfluxMeasurement]]
  * represent a single measurement, along with all the metadata associated with
  * it, which is obtained from PostgreSQL. It is the union of a particular
  * InfluxMeasurement with its corresponding MeasurementMeta.
  *
  * == Libtrace-Bigdata ==
  *
  * The [[bigdata]] package contains representations of the output of the
  * [[https://github.com/jacobvw/libtrace-bigdata Libtrace-Bigdata]] packet
  * analysis tool, created by Jacob van Walraven. The data can be gathered from
  * InfluxDB either as it is inserted (via an InfluxSourceFunction) or
  * historical data can be queried.
  *
  * Currently, only the flow_statistics table is supported, and is represented
  * by the [[nz.net.wand.streamevmon.measurements.bigdata.Flow Flow]] class.
  * While it has many fields, most are optional. The source and destination
  * location fields are in order to support the output of the Maxmind plugin,
  * which gives geolocation for IP addresses. These can also be accessed via the
  * [[nz.net.wand.streamevmon.measurements.bigdata.Flow.Endpoint Endpoint]]
  * objects named `source` and `destination`.
  *
  * == Esmond ==
  *
  * This section describes the usage of classes representing data from the
  * ESnet perfSONAR Esmond API.
  *
  * The [[nz.net.wand.streamevmon.flink.PollingEsmondSourceFunction PollingEsmondSourceFunction]]
  * produces [[nz.net.wand.streamevmon.measurements.esmond.RichEsmondMeasurement RichEsmondMeasurements]].
  * These represent time series entries, which are either raw data or a summary
  * over time thereof.
  *
  * Each type of EsmondMeasurement represents one or more `eventType`s, depending
  * on the format of their `value` field. Their `stream` field is based directly
  * on their `metadataKey`, since it is unique for a particular data stream.
  *
  * Each type of EsmondMeasurement may or may not extend HasDefault or
  * CsvOutputable, since their value types may be more complicated. For example,
  * the [[nz.net.wand.streamevmon.measurements.esmond.Simple Simple]] esmond
  * measurement supports both, as its value is just a double. The
  * [[nz.net.wand.streamevmon.measurements.esmond.PacketTrace PacketTrace]],
  * on the other hand, has a much more complex value type and extends neither.
  *
  * == Latency TS I ==
  *
  * This section describes the classes created to represent data in the Latency
  * TS I dataset ([[https://wand.net.nz/wits/latency/1/]]). These measurements
  * date from 2014 and there appear to have been some changes in AMP ICMP
  * measurement repsentation between then and the time of writing this document.
  * As such, a unique representation has been created for this dataset, along
  * with one for Smokeping data.
  *
  * Both of these types inherit from RichMeasurement, so they can be used by any
  * tool which expects regular AMP measurements. They cannot be enriched,
  * nor do they have any corresponding MeasurementMeta entries.
  *
  * Use the [[nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * and [[nz.net.wand.streamevmon.flink.LatencyTSSmokepingFileInputFormat LatencyTSSmokepingFileInputFormat]]
  * functions to ingest this dataset, as shown in [[nz.net.wand.streamevmon.runners.examples.LatencyTSToCsvPrinter LatencyTSToCsvPrinter]].
  */
package object measurements {}
