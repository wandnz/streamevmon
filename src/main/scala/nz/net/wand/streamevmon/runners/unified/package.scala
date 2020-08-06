package nz.net.wand.streamevmon.runners

/** This package contains the main entrypoint for this program. It reads a
  * desired flow configuration from a YAML file, then constructs and executes
  * that configuration as a Flink pipeline.
  *
  * An arbitrary number of sources, detectors, and sinks are supported. They are
  * represented as a directed acyclic graph (DAG). See CONFIGURING_FLOWS.md for
  * details on how to configure this package.
  */
package object unified {}
