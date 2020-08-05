package nz.net.wand.streamevmon

/** Contains entrypoints that run Flink pipelines of various configurations.
  *
  * [[runners.unified.YamlDagRunner YamlDagRunner]] is the default entrypoint.
  * It will run every supported detector according to the program config.
  *
  * [[runners.detectors]] contains runners for individual detectors, which
  * are less configurable than the default entrypoint. Code changes will be
  * needed if you want to change the data source, for example.
  *
  * [[runners.examples]] contains runners which demonstrate potentially
  * interesting applications. For example, the
  * [[nz.net.wand.streamevmon.runners.examples.MetricGatherer MetricGatherer]]
  * demonstrates gathering some statistic summaries over a tumbling window of
  * measurements.
  */
package object runners {}
