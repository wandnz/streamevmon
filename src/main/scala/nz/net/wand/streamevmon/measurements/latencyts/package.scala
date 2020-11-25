package nz.net.wand.streamevmon.measurements

/** This package contains representations of data from the
  * [[https://wand.net.nz/wits/latency/1/ Latency TS I]] dataset. While part of
  * the dataset is AMP ICMP measurements, there have been some changes in the
  * format of ICMP measurements since this dataset was gathered in 2014 and the
  * present. As such, we need a unique representation.
  *
  * Both of these types inherit from
  * [[nz.net.wand.streamevmon.measurements.traits.RichMeasurement RichMeasurement]],
  * [[nz.net.wand.streamevmon.measurements.traits.HasDefault HasDefault]], and
  * [[nz.net.wand.streamevmon.measurements.traits.CsvOutputable CsvOutputable]],
  * meaning they can be used in a wide variety of detectors.
  *
  * Use the [[nz.net.wand.streamevmon.flink.sources.LatencyTSAmpFileInputFormat LatencyTSAmpFileInputFormat]]
  * and [[nz.net.wand.streamevmon.flink.sources.LatencyTSSmokepingFileInputFormat LatencyTSSmokepingFileInputFormat]]
  * functions to ingest this dataset, as shown in [[nz.net.wand.streamevmon.runners.examples.LatencyTSToCsvPrinter LatencyTSToCsvPrinter]].
  */
package object latencyts {}
