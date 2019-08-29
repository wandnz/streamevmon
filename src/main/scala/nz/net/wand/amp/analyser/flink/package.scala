package nz.net.wand.amp.analyser

/** Contains classes which can be used as part of a Flink pipeline, including
  * custom sources, processors, and sinks.
  *
  * Most new algorithms will be best implemented as a ProcessFunction (see
  * [[nz.net.wand.amp.analyser.flink.SimpleThresholdProcessFunction SimpleThresholdProcessFunction]]
  * for an example), using an existing SourceFunction and SinkFunction.
  *
  * To define the data pipeline, a new entrypoint should be created, similar to
  * [[StreamConsumer]].
  */
package object flink {}
