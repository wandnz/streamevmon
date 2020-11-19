package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.{MeasurementMetaExtractor, TracerouteAsInetPathExtractor}
import nz.net.wand.streamevmon.flink.sources.PostgresTracerouteSourceFunction
import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

import java.time.{Duration, Instant}

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

object EventGraphCorrelator {
  def main(args: Array[String]): Unit = {
    val time = System.currentTimeMillis()

    // First, set up a StreamExecutionEnvironment.
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)
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

    val metaExtractor = new MeasurementMetaExtractor[Traceroute, TracerouteMeta]
    val tracerouteMetas = pgSource.process(metaExtractor)

    val co = new TracerouteAsInetPathExtractor
    val connected = pgSource.connect(
      tracerouteMetas
        .getSideOutput(metaExtractor.outputTag)
    )

    val asInetPaths = connected.process(co)

    //pgSource.print()
    asInetPaths.print()

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
