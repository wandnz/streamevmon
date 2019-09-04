package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.measurements._

/** Contains classes representing network measurements, such as those gathered
  * by AMP or Smokeping.
  *
  * == Live AMP Data ==
  *
  * This section describes the classes created to represent data gathered by an
  * [[nz.net.wand.streamevmon.flink.InfluxSubscriptionSourceFunction InfluxSubscriptionSourceFunction]].
  * This data comes directly from InfluxDB as it is being inserted, so should
  * be a comprehensive representation of AMP measurements.
  *
  * AMP reports measurements as a set of data unique to each individual
  * measurement, along with a Stream ID used to link a series of measurements to
  * some metadata about how they were gathered. This representation is mirrored
  * here, meaning there are three types of AMP measurement object.
  *
  * - Classes which inherit from
  * [[Measurement Measurement]]
  * represent the basic measurement, complete with its stream ID. They contain
  * no metadata, but can be "enriched" using
  * [[MeasurementFactory.enrichMeasurement MeasurementFactory.enrichMeasurement]].
  * This returns an instance of a class which inherits from RichMeasurement.
  *
  * - Classes which inherit from
  * [[MeasurementMeta MeasurementMeta]]
  * represent data gathered from a database based on a measurement's stream ID.
  * This might include a test destination, a latency, or an address family.
  * These types also include the corresponding stream ID. They cannot be
  * directly enriched, but are used in order to enrich a Measurement. There is
  * not often a need to create MeasurementMeta objects yourself, as a
  * RichMeasurement contains all the same data.
  *
  * - Classes which inherit from
  * [[RichMeasurement RichMeasurement]]
  * represent a single measurement, along with all the metadata associated with
  * it. It is the union of a particular Measurement with its corresponding
  * MeasurementMeta.
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
  * Both of these types inherit from Measurement, so they can be used by any
  * tool which expects regular AMP measurements. They cannot be enriched,
  * nor do they have any corresponding MeasurementMeta entries.
  *
  * Use the [[nz.net.wand.streamevmon.flink.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * and [[nz.net.wand.streamevmon.flink.LatencyTSSmokepingFileInputFormat LatencyTSSmokepingFileInputFormat]]
  * functions to ingest this dataset, as shown in [[LatencyTSEngine]].
  *
  * @groupname amp Live AMP Measurements
  */
package object measurements {}
