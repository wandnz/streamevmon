package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.events.grouping.graph.{TracerouteAsInetPathExtractor, TraceroutePathGraph}
import nz.net.wand.streamevmon.flink.sources.PostgresTracerouteSourceFunction
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}
import nz.net.wand.streamevmon.measurements.traits.HasDefault
import nz.net.wand.streamevmon.measurements.MeasurementMetaExtractor

import java.time.{Duration, Instant}

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala._

object EventGraphCorrelator {
  def main(args: Array[String]): Unit = {
    val time = System.currentTimeMillis()

    // First, set up a StreamExecutionEnvironment.
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(Duration.ofSeconds(60).toMillis, CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(Duration.ofSeconds(600).toMillis)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(Duration.ofSeconds(10).toMillis)
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    env.setRestartStrategy(RestartStrategies.noRestart())
    env.setParallelism(1)
    env.disableOperatorChaining()

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    // We need some deterministic streams that will generate events.
    // There also needs to be some Traceroute measurements so that we can
    // actually build the graph.
    // We'll try the cauldron-collector dataset.

    val pgSource = env
      .addSource(new PostgresTracerouteSourceFunction(
        fetchHistory = Duration.between(Instant.ofEpochSecond(1582152680), Instant.now())
      ))
      .name("PostgresSource")

    val metaExtractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]
    val tracerouteMetas = pgSource.process(metaExtractor).name(metaExtractor.flinkName)

    val co = new TracerouteAsInetPathExtractor
    val connected = pgSource.connect(
      tracerouteMetas
        .getSideOutput(metaExtractor.outputTag)
    )

    val asInetPaths = connected.process(co).name(co.flinkName)

    val detector = new BaselineDetector[TracerouteWithHasDefault]

    val events = pgSource
      .map(t => new TracerouteWithHasDefault(t))
      .name("Convert to TracerouteWithHasDefault")
      .keyBy(_.stream)
      .process(detector)
      .name(detector.flinkName)

    val grapher = new TraceroutePathGraph[Event]

    val locatedEvents = events
      .keyBy(_ => 0)
      .connect(asInetPaths)
      .process(grapher)
      .name(grapher.flinkName)

    locatedEvents.print()

    // Next, set up all the detectors to use. We might as well use all of them
    // with their default settings, since it doesn't really matter how good the
    // accuracy is for this test implementation. We just want event volume.

    // Now that we have a number of event streams, we need to tie them all
    // together and pass them to our correlator. We might as well print them too.

    env.execute()

    val time2 = System.currentTimeMillis()
    println(time2 - time, "s")
  }
}

class TracerouteWithHasDefault(
  t: Traceroute
) extends Traceroute(t.stream, t.path_id, t.aspath_id, t.packet_size, t.error_type, t.error_code, t.raw_rtts, t.timestamp) with
          HasDefault {
  override var defaultValue: Option[Double] = Some(packet_size)
}
