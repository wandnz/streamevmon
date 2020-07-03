package nz.net.wand.streamevmon.runners

/** Contains entrypoints which run detectors against local data files. These
  * files will usually be from the Latency TS I AMP dataset, so will use the
  * [[nz.net.wand.streamevmon.measurements.latencyts.LatencyTSAmpICMP LatencyTSAmpICMP]]
  * measurement type, which includes both HasDefault and CsvOutputable.
  */
package object detectors {}
