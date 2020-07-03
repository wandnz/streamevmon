package nz.net.wand.streamevmon

/** Contains entrypoints corresponding to detection algorithms. Usually, the
  * entrypoint will define the Flink data processing pipeline, then execute it.
  *
  * @see [[runners.UnifiedRunner UnifiedRunner]] is the default entrypoint.
  *      It will run every supported detector according to the program config.
  * @see [[runners.detectors]] contains runners for individual detectors, which
  *      are less configurable. Code changes will be needed if you want to
  *      change the data source, for example.
  */
package object runners {}
